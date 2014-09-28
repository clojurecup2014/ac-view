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
            [ac-view.state.game.common :as gcommon]
            [ac-view.state.game.status :as gstatus]
            ))


;;; display debug info

(def debug-msg (atom nil))

(defn add-debug-msg! []
  (reset! debug-msg (p/add-text! "" 0 500 {:align "left"}))
  nil)

(defn update-debug-msg! [text]
  (set! (.-text @debug-msg) text))



;;; preparation info

(def preparing? (atom nil))

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
  (<= 3 (count @gcommon/prepared-set)))




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

(defn logical-y->anchor-y [basesize y]
  (+ (/ y basesize) 0.5))

(defn- add-block-to-geo! [theta logical-y]
  (let [b (p/add-sprite!  :step
                         0 0
                         gcommon/block-size gcommon/block-size
                         0.5 (logical-y->anchor-y gcommon/block-size logical-y))]
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
    (swap! gcommon/prepared-set conj :geo)))




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

(defn prepare-obj-layer-async! []
  (go
    (reset! cat-assets nil)
    (let []
      (dotimes [i gcommon/cat-num]
        (<! (async/timeout 50))
        (let [sp (gcommon/prepare-cat-sprite! i)
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
      (let [sp (:sprite (get @cat-assets @my-cat-id))
            logical-y 300
            ]
        (.revive sp)
        (set! (.-x sp) 0)
        (set! (.-y sp) 0)
        (-> sp .-anchor (.setTo 0.5 (logical-y->anchor-y gcommon/block-size logical-y)))
        ;; TODO: add coins
        (swap! gcommon/prepared-set conj :obj)))))


(defn update-cat-sprite-position! [sp angle logical-y]
  (-> sp .-anchor (.setTo 0.5 (logical-y->anchor-y gcommon/block-size logical-y)))
  (set! (.-angle sp) angle))



(def _my-cat-angle (atom 0)) ; DUMMY FOR TEST
(defn- get-my-cat-angle []
  @_my-cat-angle)


(defn- update-preparation! []
  ;; TODO: display progress of preparation
  nil)

(defn- update-game! []
  ;; for debug
  (update-debug-msg!
    (str " INPUT-DEBUG: " @input/keys-state "\n"
         " RECEIVED-EV: " (pr-str (first @event/cat-queue))
         ))
  ;; game is alive?
  (when @my-cat-id
    (input/call-pressed-key-handler!))
  ;; update all
  (let [blackhole-x (/ @p/screen-w 2) ; TODO
        blackhole-y (/ @p/screen-h 2) ; TODO: get from my-cat's logical-y
        my-cat-angle (get-my-cat-angle)
        angle (- my-cat-angle)
        ]
    (update-cat-sprite-position!
      (:sprite (get @cat-assets @my-cat-id)) my-cat-angle 200)
    ;; TODO
    (set! (.-x @geo-layer) blackhole-x)
    (set! (.-y @geo-layer) blackhole-y)
    (set! (.-angle @geo-layer) angle)
    (set! (.-x @obj-layer) blackhole-x)
    (set! (.-y @obj-layer) blackhole-y)
    (set! (.-angle @obj-layer) angle)
    ;; DUMMY IMPLEMENTATION FOR TEST
    (let [s @input/keys-state]
      (when (and (:L s) (not (:R s)))
        (swap! _my-cat-angle dec)
        (.play (:sprite (get @cat-assets @my-cat-id)) "walk")
        (set! (.-width (:sprite (get @cat-assets @my-cat-id))) gcommon/block-size)
        nil)
      (when (and (:R s) (not (:L s)))
        (swap! _my-cat-angle inc)
        (set! (.-width (:sprite (get @cat-assets @my-cat-id))) (- gcommon/block-size))
        (.play (:sprite (get @cat-assets @my-cat-id)) "walk")
        nil)
      (when (and (not (:R s)) (not (:L s)))
        (.play (:sprite (get @cat-assets @my-cat-id)) "stay"))
      (when (:Z s)
        nil))
    nil))


(defn- update-obj-position!
  [obj angle center-x center-y]
  (let [x (+ (* (.sin js/Math (* (/ angle 180.0) (.-PI js/Math))) (:radius obj)) center-x)
        y (+ (* (.cos js/Math (* (/ angle 180.0) (.-PI js/Math))) (:radius obj) -1) center-y)]
  (set! (.-angle obj) angle)
    (set! (.-x obj) x)
    (set! (.-y obj) y)
  ))

(defn- update-coin-sprite-position-beta!
  [coin my-cat-angle center-x center-y]
  (let [coinsp (:sprite (get @coin-assets @my-cat-id))
        angle (- (:theta coin) my-cat-angle)]
    (-> coinsp .-anchor (.setTo 0.5 0.5))
    (update-obj-position! coinsp angle center-x center-y)
  ))

(defn- update-cat-sprite-position-beta!
  [cat my-cat-angle center-x center-y]
  (let [catsp (:sprite (get @cat-assets @my-cat-id))
        angle (- (:theta cat) my-cat-angle)]
    (-> (:sprite (get @cat-assets @my-cat-id)) .-anchor (.setTo 0.5 1.0))
    (update-obj-position! catsp angle center-x center-y)
    (cond
     (and (= (:moving cat) :left) (not= (:vx cat)))  (do (.play catsp "walk") (set! (.-width catsp) gcommon/block-size))
     (and (= (:moving cat) :right) (not= (:vy cat))) (do (set! (.-width catsp) (- gcommon/block-size)) (.play catsp "walk"))
     (and (= (:moving cat) :stay)) (.play catsp "stay")
     :else (.play catsp "stay")
     )
  ))

(defn- update-game-beta! []
  ;; game is alive?
 (when @my-cat-id
    (input/call-pressed-key-handler!))
 (let [
        blackhole-x (/ @p/screen-w 2) ; TODO
        blackhole-y (/ @p/screen-h 2) ; TODO: get from my-cat's logical-y
        ;;coins-data (:coins @event/test-queue)
        ;;blocks-data (:blocks @event/test-queue)
        my-cat @event/my-cat
        my-cat-angle (if (> (count my-cat) 0)
                       (:theta my-cat)
                       0)
       ]
   (set! (.-x @geo-layer) blackhole-x)
   (set! (.-y @geo-layer) blackhole-y)
   (set! (.-angle @geo-layer) my-cat-angle)
   (update-cat-sprite-position-beta! @event/cat-queue my-cat-angle blackhole-x blackhole-y)
   ;;(map (fn [c] (update-coin-sprite-position-beta! c  my-cat-angle blackhole-x blackhole-y)) coins-data)
   nil
   )
 )


(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)


(defn create [& _]
  (reset! preparing? true)
  (reset! gcommon/prepared-set #{})
  (reset! gcommon/fader (fader/make!))
  ;; TODO: Be careful to execution sequence of layers!
  (asset/add-bg!)
  (add-geo-layer!)
  (add-obj-layer!)
  (gstatus/add-status-layer!)
  (add-preparation-layer!)
  (add-debug-msg!) ; this is for debug only
  ;; prepare sprites and others
  (prepare-geo-layer-async!)
  (prepare-obj-layer-async!)
  (gstatus/prepare-status-layer-async!)
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
    (update-game-beta!)))




(def state-map
  {:preload preload
   :create create
   :update update
   })
