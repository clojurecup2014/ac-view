(ns ac-view.core
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.state :as state]
            ))

(defn ^:export bootstrap [target-id]
  (if-not js/window.WebSocket
    (js/alert "Don't support WebSocket to your browser!")
    (let [dom (d/by-id target-id)]
      (d/destroy-children! dom) ; clean up
      (p/init! 800 600 target-id "assets")
      (state/add-all!)
      (p/start-state! :bootstrap)
      )))
