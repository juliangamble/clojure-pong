; The pong in Clojure
;
; References:
; http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
; http://zetcode.com/tutorials/javagamestutorial/
; http://jng.imagine27.com/articles/2009-09-12-122605_pong_in_clojure.html

(import (javax.swing JFrame)
        (java.awt Color Dimension Toolkit)
        (java.awt.event KeyListener))

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
(def racquet-distance 10) ; How far from the side court walls
(def racquet-speed 0.3) ; How fast the racquet moves

(def new-ball {:x 100 :y 200 :sx 0.1 :sy 0})

(def ball-size 50)

(def lane-size 5)

; This atom stores if the racquet is going up (1) down (-1) or is stopped (0)
(def racquet-left-state (atom 0))
(def racquet-right-state (atom 0))

(def left-player-score (atom 0))
(def right-player-score (atom 0))

(defn colision-y?
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
    (colision-y? ball) (merge ball {:y (- window-height ball-size) :sy (* -1 (ball :sy))})
    (colision-xr? ball) (collided-xr ball)
    (colision-xl? ball) (collided-xl ball)
    ; Apply the physics, I added +1 in the step in order to avoid division by zero
    :else (merge ball {:x (+ (ball :x) (* (+ step 1) (ball :sx)))
                       :y (+ (ball :y) (* (+ step 1) (ball :sy)))
                       :sy (+ (ball :sy) (* 0.000098 step))})))

(defn update-racket
  [position state step]
    (cond
      (= state 1) (- position (* step racquet-speed))
      (= state -1) (+ position (* step racquet-speed))
      (= state 0) position))

(defn drawn
  [frame ball racquet-left-position racquet-right-position]
  (let [buffer (.getBufferStrategy frame)
        graphics (.getDrawGraphics buffer)]

    ; Clears the screen
    (.setColor graphics Color/BLACK)
    (.fillRect graphics 0 0 window-width window-height)

    ; Draw the ball
    (.setColor graphics Color/WHITE)
    (.drawOval graphics (ball :x) (ball :y) ball-size ball-size)

    ; Draw the court top lane
    (.fillRect graphics 0 (- bleacher-height lane-size) court-width lane-size)

    ; Draw the court division lane
    (.fillRect graphics (- (/ court-width 2) lane-size) bleacher-height lane-size court-height)

    ; Draw the left racket
    (.fillRect graphics racquet-distance (- racquet-left-position racquet-middle-height) racquet-width racquet-height)

    ; Draw the right racket
    (.fillRect graphics (- window-width (+ racquet-width racquet-distance)) (- racquet-right-position racquet-middle-height) racquet-width racquet-height)

    (.drawString graphics (str @left-player-score) 50 150)
    (.drawString graphics (str @right-player-score) 500 150)

    ; It is best to dispose() a Graphics object when done with it.
    (.dispose graphics)

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
          (if (= (.getKeyChar e) \a) (reset! racquet-left-state 1))
          (if (= (.getKeyChar e) \z) (reset! racquet-left-state -1))

          ; Pressing 'j' or 'm' updates the right racquet state
          (if (= (.getKeyChar e) \j) (reset! racquet-right-state 1))
          (if (= (.getKeyChar e) \m) (reset! racquet-right-state -1)))

        (keyReleased [e]
          ; Releasing the keys stops the racquet
          (if (or (= (.getKeyChar e) \a) (= (.getKeyChar e) \z))
            (reset! racquet-left-state 0))
          (if (or (= (.getKeyChar e) \j) (= (.getKeyChar e) \m))
            (reset! racquet-right-state 0)))

        (keyTyped [e])))

    ; Makes sure everything inside the frame fits
    (.validate frame)

    (.show frame)

    (loop [time start-time old-time start-time ball new-ball racket-left 400 racket-right 400]
      (let [step (- time old-time)]
        (drawn frame ball racket-left racket-right)

        ; (println ball)

        ; Since Clojure is so fast, a sleep is "required" in order to avoid 0ms time steps
        ; We need to implement a better game loop (Threads?)
        (Thread/sleep 20)

        (recur (System/currentTimeMillis)
               time
               (update-ball ball step)
               (update-racket racket-left @racquet-left-state step)
               (update-racket racket-right @racquet-right-state step))))))

(main)