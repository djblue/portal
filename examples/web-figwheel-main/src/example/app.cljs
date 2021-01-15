(ns example.app)

(defn start []
  (js/window.addEventListener "keydown" #(tap> %)))
