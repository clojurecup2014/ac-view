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


;;; TODO: Separate to manipulate every status


(def debug-msg (atom nil)) ; this is for debug

(def fader (atom nil))

(def msg-groups (atom {}))
(def geo-groups (atom {}))
(def cat-groups (atom {}))
(def status-groups (atom {}))

(def initializing? (atom nil))

(def my-cat-info (atom {}))
(def other-cats-info (atom {}))

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
  ;; TODO: Should display wait-to-initialize messages
  (let [bg (asset/add-bg!)
        bs (p/add-sprite! :1x1  0 0 @p/screen-w @p/screen-h 0 0)
        geo-group (-> @p/game .-add .group)
        cat-group (-> @p/game .-add .group)
        status-group (-> @p/game .-add .group)
        msg-group (-> @p/game .-add .group)
        msg (p/add-text! "Initializing ..." 300 300)
        status-x 730
        status-y 40
        status-y-diff 56
        ]
    ;(set! (.-x bg) 0)
    ;(set! (.-y bg) 0)
    ;(.add geo-group bg)
    (set! (.-x geo-group) (/ @p/screen-w 2))
    (set! (.-y geo-group) (/ @p/screen-h 2))
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
    (go
      ;; add geo objects to geo-group
      ;; TODO
      (swap! geo-groups assoc :hole (p/add-sprite! :hole 0 0))
      (.add geo-group (:hole @geo-groups))
      (<! (async/timeout 50))
      ;; dummy block
      (swap! geo-groups assoc :block (p/add-sprite! :1x1 0 10 10 10 0.5 -5))
      (.add geo-group (:block @geo-groups))
      (<! (async/timeout 50))
      ;; add status-object to status-group
      ;; TODO
      (swap! status-groups assoc :status-self (p/add-sprite! :status-frame-me status-x status-y))
      (swap! status-groups assoc :status-others (vec (map #(p/add-sprite! :status-frame-other status-x (+ status-y (* (inc %) status-y-diff)))
                                                          (range 9))))
      (.add status-group (:status-self @status-groups))
      (doall (map #(.add status-group %)
                  (:status-others @status-groups)))
      (<! (async/timeout 50))
      (swap! cat-groups assoc :cat0 (asset/gen-cat! :cat0))
      (set! (.-x (:cat0 @cat-groups)) 400)
      (set! (.-y (:cat0 @cat-groups)) 200)
      (.add cat-group (:cat0 @cat-groups))
      (swap! my-cat-info assoc :sprite (:cat0 @cat-groups))
      ;; TODO
      (fade-out-msg!))
    nil))

(defn- update-init! []
  ;; TODO
  nil)

(defn- game-live? []
  ;; TODO
  true)

(defn- update-game! []
  (when (game-live?)
    (input/call-pressed-key-handler!))
  (set! (.-text @debug-msg)
        (str " INPUT-DEBUG: " @input/keys-state "\n"
             " RECEIVED-EV: " (pr-str (first @event/test-queue))
             ))
  ;; implementation for test
  (let [s @input/keys-state]
    (when (and (:L s) (not (:R s)))
      ;; Dummy rotation
      (set! (.-rotation (:grp @geo-groups))
            (+ (.-rotation (:grp @geo-groups)) 0.1))
      (set! (.-width (:sprite @my-cat-info)) 32)
      (.play (:sprite @my-cat-info) "walk")
      nil)
    (when (and (:R s) (not (:L s)))
      ;; Dummy rotation
      (set! (.-rotation (:grp @geo-groups))
            (- (.-rotation (:grp @geo-groups)) 0.1))
      (set! (.-width (:sprite @my-cat-info)) -32)
      (.play (:sprite @my-cat-info) "walk")
      nil)
    (when (and (not (:R s)) (not (:L s)))
      (.play (:sprite @my-cat-info) "stay"))
    (when (:Z s)
      nil))
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
