(import (javax.swing JFrame)
        (java.awt Dimension))

(defn main []
    (let [frame (new JFrame)]
        (.setSize frame (new Dimension 400 400))
        (.show frame)))

(main)