(ns pong
  (:import (javax.swing JFrame)
           (java.awt Color Font Dimension Toolkit)
           (java.awt.event KeyListener)))

; The pong in Clojure
;
; References:
; http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
; http://zetcode.com/tutorials/javagamestutorial/
; http://jng.imagine27.com/articles/2009-09-12-122605_pong_in_clojure.html

; The window size
(def window-width 800)

; A tennis court size: 23.78m by 8.23m, which gives a proportion of 2,88
(def court-width window-width)
(def bleacher-height 200)
(def court-height (- court-width (/ court-width 2.88)))

(def window-height (+ court-height bleacher-height))

(def racquet-height (/ court-height 5))
(def racquet-middle-height (/ racquet-height 2))
(def racquet-width 10)
(def racquet-distance 10) ; How far from the court side walls
(def racquet-speed 0.3) ; How fast the racquet moves

(def lane-size 5)

(def ball-size 50)
(def new-ball {:x 100 :y (+ bleacher-height lane-size 1) :sx 0.1 :sy 0})


; This atom stores if the racquet is going up (1) down (-1) or is stopped (0)
(def racquet-left-state (atom {:up false :down false}))
(def racquet-right-state (atom {:up false :down false}))

(def left-player-score (atom 0))
(def right-player-score (atom 0))

(def score-height (/ bleacher-height 2))

(defn colision-yt?
  [ball]
  (< (ball :y) (+ bleacher-height lane-size)))

(defn colision-yb?
  [ball]
  (> (ball :y) (- window-height ball-size)))

(defn colision-xr?
  [ball]
  (> (ball :x) (- window-width ball-size)))

(defn colision-xl?
  [ball]
  (< (ball :x) 0))

(defn collided-xr
  [ball]
  (reset! left-player-score (inc @left-player-score))
  (merge ball {:x (- window-width ball-size) :sx (* -1 (ball :sx))}))

(defn collided-xl
  [ball]
  (reset! right-player-score (inc @right-player-score))
  (merge ball {:x 0 :sx (* -1 (ball :sx))}))

(defn update-ball
  [ball step]
  ; The cond form is usually a bad ideia. There should a better way to do this.
  (cond
    (colision-yt? ball) (merge ball {:y (+ bleacher-height lane-size) :sy (* -1 (ball :sy))})
    (colision-yb? ball) (merge ball {:y (- window-height ball-size) :sy (* -1 (ball :sy))})
    (colision-xr? ball) (collided-xr ball)
    (colision-xl? ball) (collided-xl ball)
    ; Apply the physics, I added +1 in the step in order to avoid division by zero
    :else (merge ball {:x (+ (ball :x) (* (+ step 1) (ball :sx)))
                       :y (+ (ball :y) (* (+ step 1) (ball :sy)))
                       :sy (+ (ball :sy) (* 0.000098 step))})))

(defn update-racket
  [position state step]
    (cond
      (and (= (state :up) true) (= (state :down) true)) position
      (= (state :up) true) (- position (* step racquet-speed))
      (= (state :down) true) (+ position (* step racquet-speed))
      :else position))

(defn drawn
  [frame ball racquet-left-position racquet-right-position fps]
  (let [buffer (.getBufferStrategy frame)
        graphics (.getDrawGraphics buffer)]

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
      (.drawString (str @left-player-score) 50 score-height)
      (.drawString (str @right-player-score) 500 score-height)

      ; Draw FPS counter
      (.setFont (new Font "Serif" (. Font PLAIN) 20))
      (.drawString (str fps) 770 20)

      ; It is best to dispose() a Graphics object when done with it.
      (.dispose))

    ; Shows the contents of the backbuffer on the screen.
    (.show buffer)))

(defn main
  []
  (let [frame (new JFrame "Clojure Pong")
        start-time (System/currentTimeMillis)]
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.setSize frame (new Dimension window-width window-height))
    (.setLocationRelativeTo frame nil)
    (.setUndecorated frame true)
    (.setResizable frame false)
    (.setVisible frame true)
    (.createBufferStrategy frame 2)

    (.addKeyListener frame
      (proxy [KeyListener] []
        (keyPressed [e]
          ; Exits when 'q' is pressed
          (if (= (.getKeyChar e) \q) (System/exit 0))

          ; Pressing 'a' or 'z' updates the left racquet state
          (if (= (.getKeyChar e) \a) (swap! racquet-left-state merge @racquet-left-state {:up true}))
          (if (= (.getKeyChar e) \z) (swap! racquet-left-state merge @racquet-left-state {:down true}))

          ; Pressing 'j' or 'm' updates the right racquet state
          (if (= (.getKeyChar e) \j) (swap! racquet-right-state merge @racquet-right-state {:up true}))
          (if (= (.getKeyChar e) \m) (swap! racquet-right-state merge @racquet-right-state {:down true})))

        (keyReleased [e]
          ; Releasing the keys stops the racquet
          (if (= (.getKeyChar e) \a) (swap! racquet-left-state merge @racquet-left-state {:up false}))
          (if (= (.getKeyChar e) \z) (swap! racquet-left-state merge @racquet-left-state {:down false}))
          (if (= (.getKeyChar e) \j) (swap! racquet-right-state merge @racquet-right-state {:up false}))
          (if (= (.getKeyChar e) \m) (swap! racquet-right-state merge @racquet-right-state {:down false})))

        (keyTyped [e])))

    ; Makes sure everything inside the frame fits
    (.validate frame)

    (.show frame)

    (loop [time start-time
           old-time start-time
           ball new-ball
           racket-left 400
           racket-right 400
           fps 0
           frame-counter 0
           one-second 0]
      (let [step (- time old-time)]
        (drawn frame ball racket-left racket-right fps)

        (recur (System/currentTimeMillis)
               time
               (update-ball ball step)
               (update-racket racket-left @racquet-left-state step)
               (update-racket racket-right @racquet-right-state step)
               (if (>= one-second 1000) frame-counter fps)
               (if (>= one-second 1000) 0 (inc frame-counter))
               (if (>= one-second 1000) 0 (+ one-second step)))))))

(main)