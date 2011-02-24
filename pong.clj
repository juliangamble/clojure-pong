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

; The court size: 23.78m by 8.23m, which gives a proportion of 2,88
(def court-width window-width)
(def bleacher-height (- window-width (/ window-width 2.88)))
(def court-height bleacher-height)

(def racquet-height (/ window-height 10))
(def racquet-middle-height (/ racquet-height 2))
(def racquet-width 10)

(def new-ball {:x 200 :y 200 :sx -0.001 :sy 0.01})

; Defines a atom to store the rackets positions
(def racquet-left-position (atom (/ window-height 2)))
(def racquet-right-position (atom (/ window-height 2)))

(defn update-ball [ball step]
    (merge ball {:x (ball :x)
                  ; Makes the ball fall, I added +1 in the step in order to avoid division by zero
                 :y (+ (ball :y) (* (+ step 1) (ball :sy)))
                 :sy (* 1.01 (ball :sy))})) ; Some aceleration :P

(defn drawn [frame time ball]
    (let [buffer (.getBufferStrategy frame)
          graphics (.getDrawGraphics buffer)]

        ; Clears the screen
        (.setColor graphics Color/BLACK)
        (.fillRect graphics 0 0 window-width window-height)

        ; Draw a ball at time/50
        (.setColor graphics Color/WHITE)
        (.drawOval graphics (ball :x) (ball :y) 10 10)

        (.fillRect graphics 0 (- window-height court-height 5) court-width 5)

        ; Draw the left racket
        (.fillRect graphics 5 (- @racquet-left-position racquet-middle-height) racquet-width racquet-height)

        ; Draw the right racket
        (.fillRect graphics (- window-width (+ racquet-width 5)) (- @racquet-right-position racquet-middle-height) racquet-width racquet-height)

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
                (drawn frame time ball)

                ; Since Clojure is so fast, a sleep is "required" in order to avoid 0ms time steps
                ; We need to implement a better game loop (Threads?)
                (Thread/sleep 20)

                (recur (System/currentTimeMillis) time (update-ball ball step))))))

(main)