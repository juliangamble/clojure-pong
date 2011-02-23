; The pong in Clojure
;
; References:
; http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
; http://zetcode.com/tutorials/javagamestutorial/
; http://jng.imagine27.com/articles/2009-09-12-122605_pong_in_clojure.html

(import (javax.swing JFrame)
        (java.awt Color Dimension Toolkit)
        (java.awt.event KeyListener))

(def screen-size (.. Toolkit getDefaultToolkit getScreenSize))

; The window size
(def width (/ (.getWidth screen-size) 2))
(def height (/ (.getHeight screen-size) 2))

(def racquet-height 50)
(def middle-racquet-height (/ racquet-height 2))

(def new-ball {:x 200 :y 200 :sx -0.001 :sy 1})

; Defines a atom to store the racket position
(def racket-pos (atom (/ height 2)))

(defn update-ball [ball time]
    (merge ball {:x (+ 200 (* (Math/sin (/ time 1000)) 100))
                 :y (+ 200 (* (Math/sin (/ time 500)) 100))}))

(defn drawn [frame time ball]
    (let [buffer (.getBufferStrategy frame)
          graphics (.getDrawGraphics buffer)]

        ; Clears the screen
        (.setColor graphics Color/BLACK)
        (.fillRect graphics 0 0 width height)

        ; Draw a ball at time/50
        (.setColor graphics Color/WHITE)
        (.drawOval graphics (ball :x) (ball :y) 10 10)

        ; Draw the racket
        (.fillRect graphics 5 (- @racket-pos middle-racquet-height) 10 racquet-height)

        ; It is best to dispose() a Graphics object when done with it.
        (.dispose graphics)

        ; Shows the contents of the backbuffer on the screen.
        (.show buffer)))

(defn main []
    (let [frame (new JFrame "Clojure Pong")
          start-time (System/currentTimeMillis)]
        (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setSize frame (new Dimension width height))
        (.setUndecorated frame true)
        (.setResizable frame false)
        (.setVisible frame true)
        (.createBufferStrategy frame 2)

        (.addKeyListener frame (proxy [KeyListener] []
            (keyPressed [e]
                ; Exits when 'q' is pressed
                (if (= (.getKeyChar e) \q) (System/exit 0) )

                ; Pressing 'a' or 'z' updates the racket position
                (if (and (< @racket-pos 375) (= (.getKeyChar e) \z))
                    (swap! racket-pos + 5))
                (if (and (> @racket-pos 25) (= (.getKeyChar e) \a))
                    (swap! racket-pos - 5)))
            (keyReleased [e])
            (keyTyped [e])))

        (.show frame)

        (loop [time 0 ball new-ball]
            (println ball)
            (drawn frame time ball)
            (recur (- (System/currentTimeMillis) start-time) (update-ball ball time)))))

(main)