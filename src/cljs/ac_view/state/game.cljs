(ns ac-view.state.game
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
            ))

(def fader (atom nil))

(def msg-groups (atom {}))
(def geo-groups (atom {}))

(def debug-msg (atom nil))

(def initializing? (atom nil))


(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)

(defn fade-out-msg! []
  ;; TODO: fade-out
  (doall (map #(.destroy %) (vals @msg-groups)))
  (reset! initializing? false))

(defn create [& _]
  (reset! initializing? true)
  (reset! fader (fader/make!))
  (asset/add-bg!)
  ;; TODO: Should display wait-to-initialize messages
  (let [bs (p/add-sprite! :1x1  0 0 @p/screen-w @p/screen-h 0 0)
        geo-group (-> @p/game .-add .group)
        msg-group (-> @p/game .-add .group)
        msg (p/add-text! "Initializing ..." 300 300)
        ]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 1)
    (.add msg-group bs)
    (.add msg-group msg)
    (reset! msg-groups {:grp msg-group :bs bs :msg msg})
    (reset! geo-groups {:grp geo-group})
    ;; this is for debug
    (reset! debug-msg (p/add-text! "" 0 500))
    ;; TODO: add geo objects to geo-group
    ;; Do initializing
    (go-loop []
      ;; TODO
      (swap! geo-groups assoc :hole (p/add-sprite! :hole (/ @p/screen-w 2) (/ @p/screen-h 2)))
      (.add geo-group (:hole @geo-groups))
      (<! (async/timeout 2000))
      ;; TODO
      (fade-out-msg!))
    nil))

(defn- update-init! []
  ;; TODO
  nil)

(defn- update-game! []
  (input/call-pressed-key-handler!)
  (set! (.-text @debug-msg) (pr-str :INPUT-DEBUG @input/keys-state))
  ;; TODO
  ;(js/alert "ok")
  nil)

(defn update [& _]
  (if @initializing?
    (update-init!)
    (update-game!)))





(def state-map
  {:preload preload
   :create create
   :update update
   })
