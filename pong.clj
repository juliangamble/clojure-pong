(import (javax.swing JFrame)
        (java.awt Dimension))

; The window size
(def width 400)
(def height 400)

(defn main []
    (let [frame (new JFrame "Clojure Pong")]
        (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setSize frame (new Dimension width height))
        (.setResizable frame false)
        (.setVisible frame true)
        (.createBufferStrategy frame 2)
        (.show frame)))

(main)