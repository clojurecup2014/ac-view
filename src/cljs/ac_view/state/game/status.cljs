(ns ac-view.state.game.status
  (:require-macros [ac-view.macros :as m]
                   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [cljs.core.async :as async :refer [>! <!]]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            [ac-view.fader :as fader]
            [ac-view.input :as input]
            [ac-view.event :as event]
            [ac-view.state.game.common :as gcommon]
            ))



;;; displaying cats status

(def status-layer (atom nil))

(defn add-status-layer! []
  (reset! status-layer (-> @p/game .-add .group))
  nil)

(def status-windows-info (atom {})) ; {status-id info-map, ...}

(defn prepare-status-layer-async! []
  (go
    (reset! status-windows-info {})
    (let [status-x 730
          cat-x 706
          status-y 40
          status-y-diff 56]
      (dotimes [i gcommon/cat-num]
        (<! (async/timeout 50))
        (let [x status-x
              y (+ status-y (* i status-y-diff))
              frame-sp (p/add-sprite! :status-frame-other x y)
              _ (.add @status-layer frame-sp)
              cat-sp (gcommon/prepare-cat-sprite! i)
              _ (.add @status-layer cat-sp)
              info {:frame-sprite frame-sp
                    :cat-sprite cat-sp
                    ;; TODO
                    }
              ]
          (set! (.-x cat-sp) cat-x)
          (set! (.-y cat-sp) y)
          ;; TODO: add more status information sprites
          ;; TODO: set kill/revive sprites
          (swap! status-windows-info assoc i info)))
      (swap! gcommon/prepared-set conj :status))))


