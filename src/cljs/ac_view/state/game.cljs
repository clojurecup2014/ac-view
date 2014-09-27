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


(def cat-num 10)

(def coin-num 10)


;;; TODO: Separate to manipulate every status




;(defn gen-cat-slot! [i]
;  {:nth 0
;   :id id
;   :sprite sprite
;   :pos pos
;   :status nil
;   :info {:score 0
;          :heart 3
;          :energy 5
;          }
;   ;; particle emitter
;   :pe {:jump (p/add-particle-emitter! :1x1)
;        :damage (p/add-particle-emitter! :1x1)
;        :get (p/add-particle-emitter! :1x1)
;        }
;   })

(defn gen-coin! [i id theta radius exist]
  {:nth i
   :id id
   :theta theta
   :radius radius
   :exist exist
   })

(defn gen-block! [id start end radius]
  {:id id
   :start start
   :end end
   :radius radius
   })


(def cat-assets (atom nil))

(defn gen-cat-assets! [number]
  (let [sp (asset/gen-cat! :cat0)
        color (nth [0xFFFFFF 0xFFFF7F 0xFF7FFF 0xFF7F7F 0x7FFFFF 0x7FFF7F 0x7F7FFF 0x7F7F7F 0x3F3F3F 0xBFBFBF] number)
        ;_ (<! (async/timeout 50))
        ;pe-jump (p/add-particle-emitter! :1x1)
        ;_ (<! (async/timeout 50))
        ;pe-damage (p/add-particle-emitter! :1x1)
        ;_ (<! (async/timeout 50))
        ;pe-get (p/add-particle-emitter! :1x1)
        ;_ (<! (async/timeout 50))
        ]
    (set! (.-tint sp) color)
    (.kill sp)
    {:sprite sp
     ;:pe-jump pe-jump
     ;:pe-damage pe-damage
     ;:pe-get pe-get
     }))

(defn setup-cat-sprites! []
  (reset! cat-assets (vec (doall (map gen-cat-assets! (range cat-num))))))


(def cat-infos (atom nil))

(defn gen-cat-infos [number]
  {:id number
   ;; NB: Use atom for val
   ;; TODO
   })

(defn setup-cat-infos []
  (reset! cat-infos (vec (doall (map gen-cat-infos (range cat-num))))))



(def my-cat-id (atom nil))





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

(defn logical-y->anchor-y [basesize y]
  (+ 0.5 (* 1 (/ y basesize))))

(defn- add-block-to-geo! [rot y]
  ;; FIXME: doesn't work
  (let [g (:grp @geo-groups)
        size 32
        b (p/add-sprite!  :step
                         0 (- y)
                         size size
                         0.5 (logical-y->anchor-y size y))]
    ;(set! (.-angle b) rot)
    (.add g b)))

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
      (add-block-to-geo! 30 50)
      (add-block-to-geo! 60 50)
      (add-block-to-geo! 60 100)
      (<! (async/timeout 50))
      ;; add status-object to status-group
      ;; TODO
      (swap! status-groups assoc :status-others (vec (map #(p/add-sprite! :status-frame-other status-x (+ status-y (* % status-y-diff)))
                                                          (range cat-num))))
      (doall (map #(.add status-group %)
                  (:status-others @status-groups)))
      ;; This is for player (override one of :status-others)
      (swap! status-groups assoc :status-self (p/add-sprite! :status-frame-me status-x status-y))
      (.add status-group (:status-self @status-groups))
      (<! (async/timeout 50))
      ;; Cats
      (setup-cat-sprites!)
      (reset! my-cat-id 0)
      (let [sp (:sprite (nth @cat-assets 0))]
        (.revive sp)
        (set! (.-x sp) 400)
        (set! (.-y sp) 200)
        )
      (doall (map #(.add cat-group (:sprite %))
                  @cat-assets))
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
  ;; for debug
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
      (set! (.-width (:sprite (nth @cat-assets @my-cat-id))) 32)
      (.play (:sprite (nth @cat-assets @my-cat-id)) "walk")
      nil)
    (when (and (:R s) (not (:L s)))
      ;; Dummy rotation
      (set! (.-rotation (:grp @geo-groups))
            (- (.-rotation (:grp @geo-groups)) 0.1))
      (set! (.-width (:sprite (nth @cat-assets @my-cat-id))) -32)
      (.play (:sprite (nth @cat-assets @my-cat-id)) "walk")
      nil)
    (when (and (not (:R s)) (not (:L s)))
      (.play (:sprite (nth @cat-assets @my-cat-id)) "stay"))
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
