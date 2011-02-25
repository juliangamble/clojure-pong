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
(def window-width 600)
(def window-height window-width)

; A tennis court size: 23.78m by 8.23m, which gives a proportion of 2,88
(def court-width window-width)
(def court-height (- window-width (/ window-width 2.88)))
(def bleacher-height (- window-height court-height))

(def racquet-height (/ court-height 5))
(def racquet-middle-height (/ racquet-height 2))
(def racquet-width 10)
(def racquet-distance 10) ; How far from the side court walls

(def new-ball {:x 200 :y 200 :sx 0.1 :sy 0})

(def ball-size 50)

(def lane-size 5)

; Defines a atom to store the rackets positions
(def racquet-left-position (atom (+ bleacher-height (/ court-height 2))))
(def racquet-right-position (atom (+ bleacher-height (/ court-height 2))))

(defn colision-y? [ball]
    (> (ball :y) (- window-width ball-size)))

(defn colision-xr? [ball]
    (> (ball :x) (- window-height ball-size)))

(defn colision-xl? [ball]
    (< (ball :x) 0))

(defn update-ball [ball step]
    (cond
        (colision-y? ball) (merge ball {:y (- window-width ball-size) :sy (* -1 (ball :sy))})
        (colision-xr? ball) (merge ball {:x (- window-height ball-size) :sx (* -1 (ball :sx))})
        (colision-xl? ball) (merge ball {:x ball-size :sx (* -1 (ball :sx))})
        ; Apply the physics, I added +1 in the step in order to avoid division by zero
        :else (merge ball {:x (+ (ball :x) (* (+ step 1) (ball :sx)))
                           :y (+ (ball :y) (* (+ step 1) (ball :sy)))
                           :sy (+ (ball :sy) (* 0.000098 step))})))

(defn drawn [frame ball]
    (let [buffer (.getBufferStrategy frame)
          graphics (.getDrawGraphics buffer)]

        ; Clears the screen
        (.setColor graphics Color/BLACK)
        (.fillRect graphics 0 0 window-width window-height)

        ; Draw the ball
        (.setColor graphics Color/WHITE)
        (.drawOval graphics (ball :x) (ball :y) ball-size ball-size)

        ; Draw the court top lane
        (.fillRect graphics 0 (- window-height court-height lane-size) court-width lane-size)

        ; Draw the court division lane
        (.fillRect graphics (- (/ court-width 2) lane-size) bleacher-height lane-size court-height)

        ; Draw the left racket
        (.fillRect graphics racquet-distance (- @racquet-left-position racquet-middle-height) racquet-width racquet-height)

        ; Draw the right racket
        (.fillRect graphics (- window-width (+ racquet-width racquet-distance)) (- @racquet-right-position racquet-middle-height) racquet-width racquet-height)

        ; It is best to dispose() a Graphics object when done with it.
        (.dispose graphics)

        ; Shows the contents of the backbuffer on the screen.
        (.show buffer)))

(defn main []
    (let [frame (new JFrame "Clojure Pong")
          start-time (System/currentTimeMillis)]
        (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setSize frame (new Dimension window-width window-height))
        (.setLocationRelativeTo frame nil)
        (.setUndecorated frame true)
        (.setResizable frame false)
        (.setVisible frame true)
        (.createBufferStrategy frame 2)

        (.addKeyListener frame (proxy [KeyListener] []
            (keyPressed [e]
                ; Exits when 'q' is pressed
                (if (= (.getKeyChar e) \q) (System/exit 0) )

                ; Pressing 'a' or 'z' updates the left racket position
                (if (and (< @racquet-left-position (- window-height racquet-middle-height)) (= (.getKeyChar e) \z))
                    (swap! racquet-left-position + 5))
                (if (and (> @racquet-left-position 25) (= (.getKeyChar e) \a))
                    (swap! racquet-left-position - 5))

                ; Pressing 'j' or 'm' updates the right racket position
                (if (and (< @racquet-right-position (- window-height racquet-middle-height)) (= (.getKeyChar e) \m))
                    (swap! racquet-right-position + 5))
                (if (and (> @racquet-right-position 25) (= (.getKeyChar e) \j))
                    (swap! racquet-right-position - 5)))

            (keyReleased [e])
            (keyTyped [e])))

        ; Makes sure everything inside the frame fits
        (.validate frame)

        (.show frame)

        (loop [time start-time old-time start-time ball new-ball]
            (let [step (- time old-time)]
                (drawn frame ball)

                (println ball)

                ; Since Clojure is so fast, a sleep is "required" in order to avoid 0ms time steps
                ; We need to implement a better game loop (Threads?)
                (Thread/sleep 20)

                (recur (System/currentTimeMillis) time (update-ball ball step))))))

(main)