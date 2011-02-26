(ns pong
  (:import (javax.swing JFrame)
           (java.awt Color Font Dimension Toolkit)
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

; The window size
(def window-width 800)

; A tennis court size: 23.78m by 8.23m, which gives a proportion of 2,88
(def court-width window-width)
(def bleacher-height 200)
(def court-height (- court-width (/ court-width 2.88)))

(def lane-size 5)
(def window-height (+ court-height bleacher-height))
(def score-height (/ bleacher-height 2))

; The racquet properties
(def racquet-height (/ court-height 5))
(def racquet-middle-height (/ racquet-height 2))
(def racquet-width 10)
(def racquet-distance 10) ; How far from the court side walls
(def racquet-speed 0.3) ; How fast the racquet moves

; This atom stores if the racquet is going up (1) down (-1) or is stopped (0)
(def racquet-left-state (atom {:up false :down false}))
(def racquet-right-state (atom {:up false :down false}))

(def ball-size 10)

; Game
(def new-game {:ball {:x 100 :y (+ bleacher-height lane-size 1) :sx 0.2 :sy 0.2}
               :player-left-score 0
               :player-right-score 0
               :racquet-left-pos 400
               :racquet-right-pos 400})

;;;;;;;;;;;;;;;;; Colision checking ;;;;;;;;;;;;;;;;;
(defn colision-top?
  [ball]
  (< (ball :y) (+ bleacher-height lane-size)))

(defn colision-bottom?
  [ball]
  (> (ball :y) (- window-height ball-size)))

(defn colision-right?
  [ball]
  (> (ball :x) (- window-width ball-size)))

(defn colision-left?
  [ball]
  (< (ball :x) 0))

(defn colision-racquet-left?
  [ball racquet]
  (let [top (- racquet racquet-middle-height)
        bottom (+ racquet racquet-middle-height)]
    (and (< (ball :x) (+ racquet-distance racquet-width))
         (> (ball :y) top)
         (< (ball :y) bottom))))

(defn colision-racquet-right?
  [ball racquet]
  (let [top (- racquet racquet-middle-height)
        bottom (+ racquet racquet-middle-height)]
    (and (> (ball :x) (- window-width ball-size racquet-width racquet-distance))
         (> (ball :y) top)
         (< (ball :y) bottom))))

;;;;;;;;;;;;;;;;; Colision actions ;;;;;;;;;;;;;;;;;
(defn collided-right
  [game]
  (let [ball (game :ball)]
    (merge game {:ball (merge ball {:x (- window-width ball-size)
                                    :sx (* -1 (ball :sx))})
                 :player-left-score (inc (game :player-left-score))})))

(defn collided-left
  [game]
  (let [ball (game :ball)]
    (merge game {:ball (merge ball {:x 0
                                    :sx (* -1 (ball :sx))})
                 :player-right-score (inc (game :player-right-score))})))

;;;;;;;;;;;;;;;;; Object updates ;;;;;;;;;;;;;;;;;
(defn update-ball
  [game step]
  (let [ball (game :ball)
        racquet-left (game :racquet-left-pos)
        racquet-right (game :racquet-left-pos)]
    ; The cond form is usually a bad ideia. There should a better way to do this.
    (cond
      ; This requires some serious DRY
      (colision-racquet-left? ball racquet-left) (merge ball {:x (+ racquet-distance racquet-width) :sx (* -1 (ball :sx))})
      (colision-racquet-right? ball racquet-right) (merge ball {:x (- window-width ball-size racquet-width racquet-distance) :sx (* -1 (ball :sx))})
      (colision-top? ball) (merge ball {:y (+ bleacher-height lane-size) :sy (* -1 (ball :sy))})
      (colision-bottom? ball) (merge ball {:y (- window-height ball-size) :sy (* -1 (ball :sy))})
      ; Apply the physics
      :else (merge ball {:x (+ (ball :x) (* step (ball :sx)))
                         :y (+ (ball :y) (* step (ball :sy)))}))))

(defn update-racquet
  [position state step]
    (let [top (+ bleacher-height racquet-middle-height)
          bottom (- window-height racquet-middle-height)]
      (cond
        ; Collisions
        (< position top) top
        (> position bottom) bottom
        ; Position updates
        (and (= (state :up) true) (= (state :down) true)) position
        (= (state :up) true) (- position (* step racquet-speed))
        (= (state :down) true) (+ position (* step racquet-speed))
        :else position)))

(defn update-game
  [game step]
  (let [ball (update-ball game step)
        racquet-left (update-racquet (game :racquet-left-pos) @racquet-left-state step)
        racquet-right (update-racquet (game :racquet-right-pos) @racquet-right-state step)]

    (cond
      (colision-right? ball) (collided-right game)
      (colision-left? ball) (collided-left game)
      :else
      (merge game {:ball ball
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
      (.fillRect 0 0 window-width window-height)

      ; Draw the ball
      (.setColor Color/WHITE)
      (.drawOval (ball :x) (ball :y) ball-size ball-size)

      ; Draw the court top lane
      (.fillRect 0 (- bleacher-height lane-size) court-width lane-size)

      ; Draw the court division lane
      (.fillRect (- (/ court-width 2) lane-size) bleacher-height lane-size court-height)

      ; Draw both racquets
      (.fillRect racquet-distance (- racquet-left-position racquet-middle-height) racquet-width racquet-height)
      (.fillRect (- window-width (+ racquet-width racquet-distance)) (- racquet-right-position racquet-middle-height) racquet-width racquet-height)

      ; Draw both scores
      (.setFont (new Font "Serif" (. Font PLAIN) 50))
      (.drawString (str (game :player-left-score)) 50 score-height)
      (.drawString (str (game :player-right-score)) 500 score-height)

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

(defn main
  []
  (let [frame (new JFrame "Clojure Pong")
        start-time (System/currentTimeMillis)]
    (doto frame
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setSize (new Dimension window-width window-height))
      (.setLocationRelativeTo nil)
      (.setUndecorated true)
      (.setResizable false)
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
           game new-game
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