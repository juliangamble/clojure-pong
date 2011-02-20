; The pong in Clojure
;
; References:
; http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
; http://zetcode.com/tutorials/javagamestutorial/

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

        ; It is best to dispose() a Graphics object when done with it.
        (.dispose graphics) 

        ; Shows the contents of the backbuffer on the screen.
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