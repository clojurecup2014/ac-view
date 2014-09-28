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
          status-y-diff 56
          debug-text-x (- status-x 200)
          index-x (- status-x 70)
          ]
      (dotimes [i gcommon/cat-num]
        (<! (async/timeout 1))
        (let [x status-x
              y (+ status-y (* i status-y-diff))
              frame-sp (p/add-sprite! :status-frame-other x y)
              _ (.add @status-layer frame-sp)
              cat-sp (gcommon/prepare-cat-sprite! i)
              _ (set! (.-x cat-sp) cat-x)
              _ (set! (.-y cat-sp) y)
              _ (.add @status-layer cat-sp)
              loc-debug-text (p/add-text! "" debug-text-x y)
              _ (.add @status-layer loc-debug-text)
              index-sp (asset/gen-numbers!) ; index number
              _ (set! (.-x index-sp) index-x)
              _ (set! (.-y index-sp) y)
              _ (asset/set-numbers! index-sp (inc i))
              _ (.add @status-layer index-sp)
              _ (<! (async/timeout 1))
              heart-sps (doall
                          (map
                            (fn [ii]
                              (let [sp (asset/gen-heart!)
                                    item-x (+ x 6 (* 14 ii))
                                    item-y (+ y -10)]
                                (set! (.-x sp) item-x)
                                (set! (.-y sp) item-y)
                                (.play sp "on")
                                (.add @status-layer sp)))
                            (range 3)))
              _ (<! (async/timeout 1))
              energy-sps (doall
                           (map
                             (fn [ii]
                               (let [sp (asset/gen-energy!)
                                     item-x (+ x 6 (* 14 ii))
                                     item-y (+ y 1)]
                                 (set! (.-x sp) item-x)
                                 (set! (.-y sp) item-y)
                                 (.play sp "on")
                                 (.add @status-layer sp)))
                             (range 3)))
              _ (<! (async/timeout 1))
              ;; TODO: score-text using bitmap font
              score-text-x (+ x 0)
              score-text-y (+ y 10)
              score-text (p/add-text! "0" score-text-x score-text-y)
              _ (.add @status-layer score-text)
              info {:frame-sprite frame-sp
                    :cat-sprite cat-sp
                    :loc-debug-text loc-debug-text
                    :index-sprite index-sp
                    :heart-sprites heart-sps
                    :energy-sprites energy-sps
                    :score-text score-text
                    }
              ]
          ;; TODO: add more status information sprites
          ;; TODO: set kill/revive sprites
          (swap! status-windows-info assoc i info)))
      (swap! gcommon/prepared-set conj :status))))




;;; TODO: my status window tinting





(defn update-status-window! [idx & args]
  ;; TODO
  nil)


