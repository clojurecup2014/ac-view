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
            [ac-view.event :as event]
            ))

(def debug-msg (atom nil)) ; this is for debug

(def fader (atom nil))

(def msg-groups (atom {}))
(def geo-groups (atom {}))
(def cat-groups (atom {}))
(def status-groups (atom {}))

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
        cat-group (-> @p/game .-add .group)
        status-group (-> @p/game .-add .group)
        msg-group (-> @p/game .-add .group)
        msg (p/add-text! "Initializing ..." 300 300)
        status-x 730
        status-y 40
        status-y-diff 56
        ]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 1)
    (.add msg-group bs)
    (.add msg-group msg)
    (reset! msg-groups {:grp msg-group :bs bs :msg msg})
    (reset! geo-groups {:grp geo-group})
    (reset! cat-groups {:grp cat-group})
    (reset! status-groups {:grp status-group})
    ;; this is for debug
    (reset! debug-msg (p/add-text! "" 0 500 {:align "left"}))
    ;; Do initializing
    (go-loop []
      ;; add geo objects to geo-group
      ;; TODO
      (swap! geo-groups assoc :hole (p/add-sprite! :hole (/ @p/screen-w 2) (/ @p/screen-h 2)))
      (.add geo-group (:hole @geo-groups))
      ;; add status-object to status-group
      ;; TODO
      (swap! status-groups assoc :status-self (p/add-sprite! :status-frame-me status-x status-y))
      (swap! status-groups assoc :status-others (vec (map #(p/add-sprite! :status-frame-other status-x (+ status-y (* (inc %) status-y-diff)))
                                                          (range 9))))
      (.add status-group (:status-self @status-groups))
      (doall (map #(.add status-group %)
                  (:status-others @status-groups)))
      (<! (async/timeout 2000))
      ;; TODO
      (fade-out-msg!))
    nil))

(defn- update-init! []
  ;; TODO
  nil)

(defn- update-game! []
  (input/call-pressed-key-handler!)
  (set! (.-text @debug-msg)
        (str " INPUT-DEBUG: " @input/keys-state "\n"
             " RECEIVED-EV: " (pr-str (first @event/test-queue))
             ))
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
