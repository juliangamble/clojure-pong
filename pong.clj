(import (javax.swing JFrame)
        (java.awt Color Dimension))

; The window size
(def width 400)
(def height 400)

(defn drawn [frame]
    (let [buffer (.getBufferStrategy frame)
          graphics (.getDrawGraphics buffer)]
        (.setColor graphics Color/BLACK)
        (.drawLine graphics 0 0 100 100)
        (.show buffer)))

(defn main []
    (let [frame (new JFrame "Clojure Pong")]
        (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setSize frame (new Dimension width height))
        (.setUndecorated frame true)
        (.setResizable frame false)
        (.setVisible frame true)
        (.createBufferStrategy frame 2)
        (.show frame)

        (drawn frame)))

(main)