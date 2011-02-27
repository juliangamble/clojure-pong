(ns pong
  (:import (javax.swing JFrame)
           (java.awt Color Font Dimension GraphicsEnvironment Toolkit)
           (java.awt.event KeyListener)))

; The pong in Clojure
;
; by Cesar Canassa and Julio Nobrega
;
; 2011-02-26
;
; References:
; http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
; http://zetcode.com/tutorials/javagamestutorial/
; http://jng.imagine27.com/articles/2009-09-12-122605_pong_in_clojure.html
; http://www.bestinclass.dk/index.clj/2010/10/taking-uncle-bob-to-school.html
; http://www.youtube.com/watch?v=nzHDQvzUXL0

; This atom stores if the racquet is going up (1) down (-1) or is stopped (0)
(def racquet-left-state (atom {:up false :down false}))
(def racquet-right-state (atom {:up false :down false}))

;;;;;;;;;;;;;;;;; Colision checking ;;;;;;;;;;;;;;;;;
(defn colision-top?
  [game]
  (< ((game :ball) :y) (+ (game :bleacher-height) (game :lane-size))))

(defn colision-bottom?
  [game]
  (> ((game :ball) :y) (- (game :window-height) (game :ball-size))))

(defn colision-right?
  [game]
  (> ((game :ball) :x) (- (game :window-width) (game :ball-size))))

(defn colision-left?
  [game]
  (< ((game :ball) :x) 0))

(defn colision-racquet-left?
  [game]
  (let [ball (game :ball)
        racquet (game :racquet-left-pos)
        top (- racquet (game :racquet-middle-height))
        bottom (+ racquet (game :racquet-middle-height))]
    (and (< (ball :x) (+ (game :racquet-distance) (game :racquet-width)))
         (> (ball :y) top)
         (< (ball :y) bottom))))

(defn colision-racquet-right?
  [game]
  (let [ball (game :ball)
        racquet (game :racquet-right-pos)
        top (- racquet (game :racquet-middle-height))
        bottom (+ racquet (game :racquet-middle-height))]
    (and (> (ball :x) (- (game :window-width) (game :ball-size) (game :racquet-width) (game :racquet-distance)))
         (> (ball :y) top)
         (< (ball :y) bottom))))

;;;;;;;;;;;;;;;;; Colision actions ;;;;;;;;;;;;;;;;;
(defn collided-right
  [game]
  (let [ball (game :ball)]
    (merge game {:ball (merge ball {:x (- (game :window-width) (game :ball-size))
                                    :sx (* -1 (ball :sx))})
                 :player-left-score (inc (game :player-left-score))})))

(defn collided-racquet-right
  [game]
  (let [ball (game :ball)
        racquet (game :racquet-right-pos)
        hit (/ (- (ball :y) racquet) (game :racquet-middle-height))]
    (merge game {:ball (merge ball {:x (- (game :window-width) (game :ball-size) (game :racquet-width) (game :racquet-distance))
                                    :sx (* -1 (Math/cos hit) (game :speed))
                                    :sy (* (Math/sin hit) (game :speed))})})))

(defn collided-racquet-left
  [game]
  (let [ball (game :ball)
        racquet (game :racquet-left-pos)
        hit (/ (- (ball :y) racquet) (game :racquet-middle-height))]
    (merge game {:ball (merge ball {:x (+ (game :racquet-distance) (game :racquet-width))
                                    :sx (* (Math/cos hit) (game :speed))
                                    :sy (* (Math/sin hit) (game :speed))})})))

(defn collided-left
  [game]
  (let [ball (game :ball)]
    (merge game {:ball (merge ball {:x 0
                                    :sx (* -1 (ball :sx))})
                 :player-right-score (inc (game :player-right-score))})))

;;;;;;;;;;;;;;;;; Object updates ;;;;;;;;;;;;;;;;;
(defn update-ball
  [game step]
  (let [ball (game :ball)]
    ; The cond form is usually a bad ideia. There should a better way to do this.
    (cond
      ; This requires some serious DRY
      (colision-racquet-left? game) (collided-racquet-left game)
      (colision-racquet-right? game) (collided-racquet-right game)
      (colision-top? game) (merge game {:ball (merge ball {:y (+ (game :bleacher-height) (game :lane-size))
                                                           :sy (* -1 (ball :sy))})})
      (colision-bottom? game) (merge game {:ball (merge ball {:y (- (game :window-height) (game :ball-size))
                                                              :sy (* -1 (ball :sy))})})
      ; Apply the physics
      :else (merge game {:ball (merge ball {:x (+ (ball :x) (* step (ball :sx)))
                                            :y (+ (ball :y) (* step (ball :sy)))})}))))

(defn update-racquet
  [game position state step]
    (let [top (+ (game :bleacher-height)
                 (game :racquet-middle-height))
          bottom (- (game :window-height) (game :racquet-middle-height))]
      (cond
        ; Collisions
        (< position top) top
        (> position bottom) bottom
        ; Position updates
        (and (= (state :up) true) (= (state :down) true)) position
        (= (state :up) true) (- position (* step (game :racquet-speed)))
        (= (state :down) true) (+ position (* step (game :racquet-speed)))
        :else position)))

(defn update-game
  [game step]
  (let [game (update-ball game step)
        racquet-left (update-racquet game (game :racquet-left-pos) @racquet-left-state step)
        racquet-right (update-racquet game (game :racquet-right-pos) @racquet-right-state step)]

    (cond
      (colision-right? game) (collided-right game)
      (colision-left? game) (collided-left game)
      :else
      (merge game {:ball (game :ball)
                   :racquet-left-pos racquet-left
                   :racquet-right-pos racquet-right}))))

;;;;;;;;;;;;;;;;; Draw, Keypress, Main loop ;;;;;;;;;;;;;;;;;
(defn drawn
  [frame game fps]
  (let [buffer (.getBufferStrategy frame)
        graphics (.getDrawGraphics buffer)
        ball (game :ball)
        racquet-left-position (game :racquet-left-pos)
        racquet-right-position (game :racquet-right-pos)]

    (doto graphics
      ; Clears the screen
      (.setColor Color/BLACK)
      (.fillRect 0 0 (game :window-width) (game :window-height))

      ; Draw the ball
      (.setColor Color/WHITE)

      (.fillOval (ball :x) (ball :y) (game :ball-size) (game :ball-size))

      ; Draw the court top lane
      (.fillRect 0 (- (game :bleacher-height) (game :lane-size)) (game :court-width) (game :lane-size))

      ; Draw the court division lane
      (.fillRect (- (/ (game :court-width) 2) (game :lane-size))
                 (game :bleacher-height)
                 (game :lane-size)
                 (game :court-height))

      ; Draw both racquets
      (.fillRect (game :racquet-distance)
                 (- (game :racquet-left-pos) (game :racquet-middle-height))
                 (game :racquet-width) (game :racquet-height))
      (.fillRect (- (game :window-width) (+ (game :racquet-width) (game :racquet-distance)))
                 (- (game :racquet-right-pos) (game :racquet-middle-height))
                 (game :racquet-width)
                 (game :racquet-height))

      ; Draw both scores
      (.setFont (new Font "Serif" (. Font PLAIN) 50))
      (.drawString (str (game :player-left-score)) 50 (game :score-height))
      (.drawString (str (game :player-right-score)) 500 (game :score-height))

      ; Draw FPS counter
      (.setFont (new Font "Serif" (. Font PLAIN) 20))
      (.drawString (str fps) 770 20)

      ; It is best to dispose() a Graphics object when done with it.
      (.dispose))

    ; Shows the contents of the backbuffer on the screen.
    (.show buffer)))

(defn swap-key
  [atom struct]
  (swap! atom merge @atom struct))

(defn handle-keypress
  [e]
    (case e
      ; Exits when 'q' is pressed
      \q (System/exit 0)

      ; Pressing 'a' or 'z' updates the left racquet state
      \a (swap-key racquet-left-state {:up true})
      \z (swap-key racquet-left-state {:down true})

      ; Pressing 'j' or 'm' updates the right racquet state
      \j (swap-key racquet-right-state {:up true})
      \m (swap-key racquet-right-state {:down true})
      nil))

(defn handle-keyrelease
  [e]
    ; Releasing the keys stops the racquet
    (case e
      \a (swap-key racquet-left-state {:up false})
      \z (swap-key racquet-left-state {:down false})
      \j (swap-key racquet-right-state {:up false})
      \m (swap-key racquet-right-state {:down false})
      nil))

(defn new-game
  [width height]
  (let [bleacher-height 200
        court-height (- height bleacher-height)
        racquet-height (/ court-height 5)]
    {:ball {:x 100 :y 300 :sx 0.2 :sy -0.2}
      :speed 0.5
      :player-left-score 0
      :player-right-score 0
      :racquet-left-pos 400
      :racquet-right-pos 400

      :window-width width
      :window-height height
      :bleacher-height bleacher-height

      :lane-size 5
      :ball-size 10
      :score-height (/ bleacher-height 2)

      :court-width width
      :court-height court-height

      :racquet-distance 10 ; How far from the court side walls
      :racquet-speed 0.3 ; How fast the racquet moves
      :racquet-width 10
      :racquet-height racquet-height
      :racquet-middle-height (/ racquet-height 2)}))

(defn main
  []
  (let [frame (new JFrame "Clojure Pong")
        start-time (System/currentTimeMillis)
        toolkit (. Toolkit getDefaultToolkit)
        ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (. ge getDefaultScreenDevice)]
    (doto frame
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setUndecorated true)
      ;(.setIgnoreRepaint true) ; This is supposed to give us extra FPS, but I see to diference
      (.setResizable false))

    (.setFullScreenWindow gd frame)

    (doto frame
      (.setVisible true)
      (.createBufferStrategy 2)

      (.addKeyListener
        (proxy [KeyListener] []
          (keyPressed [e]
            (handle-keypress (.getKeyChar e)))
          (keyReleased [e]
            (handle-keyrelease (.getKeyChar e)))
          (keyTyped [e])))

      ; Makes sure everything inside the frame fits
      (.validate)

      (.show))

    (loop [time start-time
           old-time start-time
           game (new-game (.. toolkit getScreenSize width) (.. toolkit getScreenSize height))
           fps 0
           frame-counter 0
           one-second 0]
      (let [step (- time old-time)
            new-fps? (>= one-second 1000)]

        (drawn frame game fps)

        (recur (System/currentTimeMillis)
               time
               (update-game game step)
               (if new-fps? frame-counter fps)
               (if new-fps? 0 (inc frame-counter))
               (if new-fps? 0 (+ one-second step)))))))

(main)