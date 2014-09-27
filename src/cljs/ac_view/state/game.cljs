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
        ;hole (p/add-sprite! :hole screen-w-half screen-h-half)
        msg-group (-> @p/game .-add .group)
        geo-group (-> @p/game .-add .group)
        msg (p/add-text! "Initializing ..." 300 300)
        ]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 1)
    (set! (.-z bs) 1000)
    (reset! msg-groups {:grp msg-group :bs bs :msg msg})
    ;; this is for debug
    (reset! debug-msg (p/add-text! "" 0 500))
    ;; TODO: add geo objects to geo-group
    ;; Do initializing
    (go-loop []
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
