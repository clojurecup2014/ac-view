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


(def block-size 32)

(def cat-num 10)

(def coin-num 10)

(def fader (atom nil))



;;; display debug info

(def debug-msg (atom nil))

(defn add-debug-msg! []
  (reset! debug-msg (p/add-text! "" 0 500 {:align "left"}))
  nil)

(defn update-debug-msg! [text]
  (set! (.-text @debug-msg) text))



;;; preparation info

(def preparing? (atom nil))
(def prepared-set (atom #{}))

(def preparation-layer (atom nil))

(defn add-preparation-layer! []
  (reset! preparation-layer (-> @p/game .-add .group))
  (let [bs (p/add-sprite! :1x1  0 0 @p/screen-w @p/screen-h 0 0)
        msg (p/add-text! "Initializing ..." 300 300)
        ]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 1)
    (.add @preparation-layer bs)
    (.add @preparation-layer msg)
    nil))

(defn fadeout-preparation-layer! [handle]
  ;; TODO: fade-out
  (set! (.-visible @preparation-layer) false)
  (handle))

(defn prepare-is-all-done? []
  (console.log (count @prepared-set))
  (<= 3 (count @prepared-set)))






;;; geo info (including blocks)

(def geo-layer (atom nil)) ; (0,0) is blackhole

(def geo-theta (atom 0)) ; = - theta of my cat
(def geo-x (atom 0)) ; blackhole's x in screen coordination (= cat's x in screen coordination)
(def geo-y (atom 0)) ; blackhole's y in screen coordination (for up/down scrolling)

(def blocks-info (atom {})) ; {block-id info-map, ...} ; logical-info



(defn add-geo-layer! []
  (reset! geo-layer (-> @p/game .-add .group))
  (set! (.-x @geo-layer) (/ @p/screen-w 2))
  (set! (.-y @geo-layer) (/ @p/screen-h 2))
  nil)

(defn- logical-y->anchor-y [basesize y]
  (+ (/ y basesize) 0.5))

(defn- add-block-to-geo! [theta logical-y]
  (let [b (p/add-sprite!  :step
                         0 0
                         block-size block-size
                         0.5 (logical-y->anchor-y block-size logical-y))]
    (set! (.-angle b) theta)
    (.add @geo-layer b)))

(defn prepare-geo-layer-async! []
  (go
    (let [hole (p/add-sprite! :hole 0 0)]
      (.add @geo-layer hole))
    (<! (async/timeout 50))
    ;; dummy block (TODO)
    (add-block-to-geo! 0 150)
    (add-block-to-geo! 30 150)
    (add-block-to-geo! 60 150)
    (add-block-to-geo! 60 200)
    (swap! prepared-set conj :geo)))




;;; obj info (including cats)

(def obj-layer (atom nil)) ; (0,0) is blackhole

(def cat-assets (atom {})) ; {cat-id info-map, ...} ; sprite-info
(def cats-info (atom {})) ; {cat-id info-map, ...} ; logical-info
(def coin-assets (atom {})) ; {coin-id info-map, ...} ; sprite-info
(def coins-info (atom {})) ; {coin-id info-map, ...} ; logical-info

(def my-cat-id (atom nil)) ; nil = game isn't live

(defn add-obj-layer! []
  (reset! obj-layer (-> @p/game .-add .group))
  nil)

;(defn- get-cat-color [i]
;  (nth [0xFFFFFF
;        0xFFFF7F
;        0xFF7FFF
;        0xFF7F7F
;        0x7FFFFF
;        0x7FFF7F
;        0x7F7FFF
;        0x7F7F7F
;        0x3F3F3F
;        0xBFBFBF] i 0xFFFFFF))

(defn prepare-cat-sprite! [i]
  (let [sp (asset/gen-cat! :cat0)
        ;color (get-cat-color i)
        ]
    ;(set! (.-tint sp) color) ; NB: tinting kill animation!
    sp))

(defn prepare-obj-layer-async! []
  (go
    (reset! cat-assets nil)
    (let []
      (dotimes [i cat-num]
        (<! (async/timeout 50))
        (let [sp (prepare-cat-sprite! i)
              ;pe-jump (p/add-particle-emitter! :1x1)
              ;_ (<! (async/timeout 50))
              ;pe-damage (p/add-particle-emitter! :1x1)
              ;_ (<! (async/timeout 50))
              ;pe-get (p/add-particle-emitter! :1x1)
              ;_ (<! (async/timeout 50))
              info {:sprite sp
                    ;:pe-jump pe-jump
                    ;:pe-damage pe-damage
                    ;:pe-get pe-get
                    }
              ]
          (.add @obj-layer sp)
          (.kill sp)
          (swap! cat-assets assoc i info)))
      (reset! my-cat-id 0) ; TODO: THIS IS FOR TEST!
      (let [sp (:sprite (get @cat-assets @my-cat-id))]
        (.revive sp)
        (set! (.-x sp) 0)
        (set! (.-y sp) -100) ; dummy
        ;; TODO: add coins
        (swap! prepared-set conj :obj)))))





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
      (dotimes [i cat-num]
        (<! (async/timeout 50))
        (let [x status-x
              y (+ status-y (* i status-y-diff))
              frame-sp (p/add-sprite! :status-frame-other x y)
              _ (.add @status-layer frame-sp)
              cat-sp (prepare-cat-sprite! i)
              _ (.add @status-layer cat-sp)
              info {:frame-sprite frame-sp
                    :cat-sprite cat-sp
                    ;; TODO
                    }
              ]
          (set! (.-x cat-sp) cat-x)
          (set! (.-y cat-sp) y)
          ;; TODO: set kill/revive sprites
          (swap! status-windows-info assoc i info)))
      (swap! prepared-set conj :status))))























(defn- update-preparation! []
  ;; TODO: display progress of preparation
  nil)

(defn- update-geo-from-my-cat-info! []
  ;; TODO
  nil)

(defn- update-game! []
  ;; for debug
  (update-debug-msg!
    (str " INPUT-DEBUG: " @input/keys-state "\n"
         " RECEIVED-EV: " (pr-str (first @event/test-queue))
         ))

  (when @my-cat-id ; game is live?
    (input/call-pressed-key-handler!)
    (update-geo-from-my-cat-info!)
    )
  (let [blackhole-x (/ @p/screen-w 2) ; TODO
        blackhole-y (/ @p/screen-h 2) ; TODO: get from my-cat's logical-y
        angle 0 ; TODO: get from my-cat (or 0)
        ]
    ;; TODO
    (set! (.-x @geo-layer) blackhole-x)
    (set! (.-y @geo-layer) blackhole-y)
    (set! (.-angle @geo-layer) angle)
    (set! (.-x @obj-layer) blackhole-x)
    (set! (.-y @obj-layer) blackhole-y)
    (set! (.-angle @obj-layer) angle)
    ;; implementation for test
    ;(let [s @input/keys-state]
    ;  (when (and (:L s) (not (:R s)))
    ;    ;; Dummy rotation
    ;    (set! (.-rotation (:layer @geo-groups))
    ;          (+ (.-rotation (:layer @geo-groups)) 0.1))
    ;    (set! (.-width (:sprite (nth @cat-assets @my-cat-id))) block-size)
    ;    (.play (:sprite (nth @cat-assets @my-cat-id)) "walk")
    ;    nil)
    ;  (when (and (:R s) (not (:L s)))
    ;    ;; Dummy rotation
    ;    (set! (.-rotation (:layer @geo-groups))
    ;          (- (.-rotation (:layer @geo-groups)) 0.1))
    ;    (set! (.-width (:sprite (nth @cat-assets @my-cat-id))) -block-size)
    ;    (.play (:sprite (nth @cat-assets @my-cat-id)) "walk")
    ;    nil)
    ;  (when (and (not (:R s)) (not (:L s)))
    ;    (.play (:sprite (nth @cat-assets @my-cat-id)) "stay"))
    ;  (when (:Z s)
    ;    nil))
    ;; TODO
    ;(js/alert "ok")
    nil))












(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)


(defn create [& _]
  (reset! preparing? true)
  (reset! prepared-set #{})
  (reset! fader (fader/make!))
  ;; TODO: Be careful to execution sequence of layers!
  (asset/add-bg!)
  (add-geo-layer!)
  (add-obj-layer!)
  (add-status-layer!)
  (add-preparation-layer!)
  (add-debug-msg!) ; this is for debug only
  ;; prepare sprites and others
  (prepare-geo-layer-async!)
  (prepare-obj-layer-async!)
  (prepare-status-layer-async!)
  (go-loop []
    (<! (async/timeout 500))
    (if (prepare-is-all-done?)
      (fadeout-preparation-layer! #(reset! preparing? false))
      (recur)))
  nil)



(defn update [& _]
  ;; TODO: wait info for start from server
  (if @preparing?
    (update-preparation!)
    (update-game!)))




(def state-map
  {:preload preload
   :create create
   :update update
   })
