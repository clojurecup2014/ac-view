(ns ac-view.core
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.state :as state]
            [ac-view.input :as input]
            [ac-view.event :as event]
            ))

(defn ^:export call-event! [event]
  (event/enqueue! event))

(defn ^:export bootstrap
  [target-id & [handle-l handle-r handle-z handle-listen]]
  (if-not js/window.WebSocket
    (js/alert "Don't support WebSocket to your browser!")
    (let [dom (d/by-id target-id)]
      (d/destroy-children! dom) ; clean up
      (p/init! 800 600 target-id "assets")
      (state/add-all!)
      (p/start-state! :bootstrap)
      (when handle-l
        (input/set-handler! :L handle-l))
      (when handle-r
        (input/set-handler! :R handle-r))
      (when handle-l
        (input/set-handler! :Z handle-z))
      (when handle-listen
        (input/set-ws-emitter! handle-listen))
      )))
