(import (javax.swing JFrame)
        (java.awt Dimension))

(defn main []
    (let [frame (new JFrame)]
        (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setSize frame (new Dimension 400 400))
        (.show frame)))

(main)