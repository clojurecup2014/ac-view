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

(def ^:private pe-lifespan 250)

(def timeout-idle-count 50)

;;; TODO use :id if pssile

(defn get-cat-id
  [c]
  (js/parseInt (last (:img c)) 10))


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
        msg (p/add-text! "Initializing ..." 350 300)
        ]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 1)
    (.add @preparation-layer bs)
    (.add @preparation-layer msg)
    nil))

(defn fadeout-preparation-layer! [handle]
  (let [t (-> @p/game .-add (.tween @preparation-layer))]
    (.to t (js-obj "y" -600) 250)
    (.add (.-onComplete t) (fn [& _]
                             (set! (.-visible @preparation-layer) false)
                             (handle)))
    (.start t)))

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
                         0.5 (+ 1 (/ logical-y 10.0)))]
    (set! (.-angle b) theta)
    (set! (.-width b) 10)
    (set! (.-height b) 10)
    (.add @geo-layer b)))

(defn prepare-geo-layer-async! []
  (go
    (let [hole (p/add-sprite! :hole 0 0)]
      (set! (.-width hole) (* 2 (:ground-y @event/global-map)))
      (set! (.-height hole) (* 2 (:ground-y @event/global-map)))
      (.add @geo-layer hole))
    (<! (async/timeout 1))
    ;; dummy block (TODO)

    (doseq [b @event/global-blocks]
      (let [half-theta (/ (* 180 5) (* (.-PI js/Math) (:radius b)))
            theta-candidate (filter #(< (+ (* % half-theta) (:start b)) (:end b)) (range 1 20 1))]
        (doseq [th theta-candidate]
          (add-block-to-geo! (+ (* th half-theta) (:start b)) (:radius b))
          )
        )
      )

    ;;(add-block-to-geo! 0 150)
    ;;(add-block-to-geo! 30 150)
    ;;(add-block-to-geo! 60 150)
    ;;(add-block-to-geo! -60 200)
    (swap! gcommon/prepared-set conj :geo)))




;;; obj info (including cats)

(def obj-layer (atom nil)) ; (0,0) is blackhole

(def cat-assets (atom {})) ; {cat-id info-map, ...} ; sprite-info
(def cats-info (atom {})) ; {cat-id info-map, ...} ; logical-info
(def cats-idle-count (atom {}))
(def coin-assets (atom {})) ; {coin-id info-map, ...} ; sprite-info
(def coins-info (atom {})) ; {coin-id info-map, ...} ; logical-info

(def my-cat-id (atom nil)) ; nil = game isn't live

(defn add-obj-layer! []
  (reset! obj-layer (-> @p/game .-add .group))
  nil)


(defn- network-error! []
  ;; TODO: Erase loading messages, and other...
  (js/alert "No room for your connection. Please retry later.")
  (js/setTimeout (fn []
                (set! (.-location js/window) "/"))
                5000)
  nil)

(defn prepare-obj-layer-async! []
  (go
    (reset! cat-assets nil)
    (let []
      (dotimes [i gcommon/cat-num]
        (<! (async/timeout 1))
        (let [sp (gcommon/prepare-cat-sprite! i)
              _ (.add @obj-layer sp)
              _ (.kill sp)
              _ (<! (async/timeout 1))
              pe-jump (p/add-particle-emitter! :cloud 100)
              ;_ (<! (async/timeout 1))
              ;pe-damage (p/add-particle-emitter! :1x1)
              ;_ (<! (async/timeout 1))
              ;pe-get (p/add-particle-emitter! :1x1)
              info {:sprite sp
                    :pe-jump pe-jump
                    ;:pe-damage pe-damage
                    ;:pe-get pe-get
                    }
              ]
          (swap! cat-assets assoc i info)))
      ;; TODO: Wait data from server when not get my-cat-id
      (reset! my-cat-id (get-cat-id @event/my-cat))
      ;(reset! my-cat-id 0) ; FOR STANDALONE DEBUG
      (if (js/isNaN @my-cat-id)
        (network-error!)
        (let [sp (:sprite (get @cat-assets @my-cat-id))
              logical-y 300
              ]
          (.revive sp)
          (set! (.-x sp) 0)
          (set! (.-y sp) 0)
          (-> sp .-anchor (.setTo 0.5 (logical-y->anchor-y gcommon/block-size logical-y)))
          ;; TODO: add coins
          (swap! gcommon/prepared-set conj :obj))))))

(defn update-cat-sprite-position! [sp angle logical-y]
  (-> sp .-anchor (.setTo 0.5 (logical-y->anchor-y gcommon/block-size logical-y)))
  (set! (.-angle sp) angle))


(defn- do-tweet! [my-cat]
  (let [tweet-url (asset/get-tweet-url (str "Your score: "
                                            (:score my-cat)
                                            " (wip)"))]
    (js/window.open tweet-url "_blank")))


(def game-over? (atom nil))
(defn- emit-game-over! [my-cat]
  ;; TODO: SE
  ;; TODO: Effect
  ;; Setup game-over screen
  (let [bs (p/add-sprite! :1x1 0 0 @p/screen-w @p/screen-h 0 0)]
    (set! (.-tint bs) 0)
    (set! (.-alpha bs) 0.5))
  (p/add-sprite! :gameover 400 200)
  (p/add-text! (str "SCORE: " (:score my-cat)) 350 400 {:font "20px monospace"})
  (let [v-x (+ 400 -62)
        v-y 500]
    (-> @p/game .-add (.button v-x v-y "menu-game-tweet" #(do-tweet! my-cat) nil 1 0)))
  (reset! game-over? true))

(defn- update-game-over! []
  ;; TODO
  nil)


(defn- update-preparation! []
  ;; TODO: display progress of preparation
  nil)



;(def _my-cat-angle (atom 0)) ; DUMMY FOR TEST
;(defn- get-my-cat-angle []
;  @_my-cat-angle)
;
;
;(defn- update-game! []
;  ;; for debug
;  (update-debug-msg!
;    (str " INPUT-DEBUG: " @input/keys-state "\n"
;         " RECEIVED-EV: " (pr-str (first @event/cat-queue))
;         ))
;  ;; game is alive?
;  (when @my-cat-id
;    (input/call-pressed-key-handler!))
;  ;; update all
;  (let [blackhole-x (/ @p/screen-w 2) ; TODO
;        blackhole-y (/ @p/screen-h 2) ; TODO: get from my-cat's logical-y
;        my-cat-angle (get-my-cat-angle)
;        angle (- my-cat-angle)
;        ]
;    (update-cat-sprite-position!
;      (:sprite (get @cat-assets @my-cat-id)) my-cat-angle 200)
;    (set! (.-x @geo-layer) blackhole-x)
;    (set! (.-y @geo-layer) blackhole-y)
;    (set! (.-angle @geo-layer) angle)
;    (set! (.-x @obj-layer) blackhole-x)
;    (set! (.-y @obj-layer) blackhole-y)
;    (set! (.-angle @obj-layer) angle)
;    (let [s @input/keys-state]
;      (when (and (:L s) (not (:R s)))
;        (swap! _my-cat-angle dec)
;        (.play (:sprite (get @cat-assets @my-cat-id)) "walk")
;        (set! (.-width (:sprite (get @cat-assets @my-cat-id))) gcommon/block-size)
;        nil)
;      (when (and (:R s) (not (:L s)))
;        (swap! _my-cat-angle inc)
;        (set! (.-width (:sprite (get @cat-assets @my-cat-id))) (- gcommon/block-size))
;        (.play (:sprite (get @cat-assets @my-cat-id)) "walk")
;        nil)
;      (when (and (not (:R s)) (not (:L s)))
;        (.play (:sprite (get @cat-assets @my-cat-id)) "stay"))
;      (when (:Z s)
;        nil))
;    nil))


(defn- update-obj-position!
  [obj angle radius center-x center-y]
  (let [x (+ (* (.sin js/Math (* (/ angle 180.0) (.-PI js/Math))) (+ radius 10)) center-x)
        y (+ (* (.cos js/Math (* (/ angle 180.0) (.-PI js/Math))) (+ radius 10) -1) center-y)]
    (set! (.-angle obj) angle)
    (set! (.-x obj) x)
    (set! (.-y obj) y)
    nil
    ))


(defn- update-cat-sprite-position-beta!
  [cat my-cat-angle center-x center-y]
  (let [catsp (:sprite (get @cat-assets (get-cat-id cat)))
        angle (- (:theta cat) my-cat-angle)]
    (-> catsp .-anchor (.setTo 0.5 1.0))
    (if (> (:life cat) 0)
      (.revive catsp)
      (.kill catsp))
    (update-obj-position! catsp angle (:radius cat) center-x center-y)
    (cond
     (and (= (:moving cat) "left") (> (.abs js/Math (:vx cat)) 0.5))  (do (.play catsp "walk") (set! (.-width catsp) gcommon/block-size))
     (and (= (:moving cat) "right") (> (.abs js/Math (:vx cat)) 0.5)) (do (set! (.-width catsp) (* gcommon/block-size -1)) (.play catsp "walk"))
     (and (= (:moving cat) "stay")) (.play catsp "stay")
     :else (.play catsp "stay")
     )
    nil
  ))


(defn- emit-jump! [cat my-cat-angle center-x center-y]
  (let [vol (if (:me cat) 1 0.5)]
    (asset/play-se! :jump vol))
  (let [theta (:theta cat 0)
        radius (:radius cat 0)
        angle (- theta my-cat-angle)
        pe (:pe-jump (@cat-assets (get-cat-id cat)))
        _ (update-obj-position! pe angle radius center-x center-y)
        p-x (.-x pe)
        p-y (.-y pe)
        ]
    (p/emit-particle! pe p-x p-y pe-lifespan 8))
  nil)

(defn- update-cat! [cat my-cat-angle center-x center-y]
  (update-cat-sprite-position-beta! cat my-cat-angle center-x center-y)
  (when (:jump cat)
    (when-let [prev-frame (gstatus/get-previous-frame-status (:id cat))]
      (when-not (:jump prev-frame)
        (emit-jump! cat my-cat-angle center-x center-y))))
  (gstatus/update-status-window! cat))

(defn- update-particles! []
  (doseq [pe (map :pe-jump (vals @cat-assets))]
    (when pe
      (.forEachAlive
        pe
        (fn [p]
          (let [alpha (/ (.-lifespan p) pe-lifespan)
                alpha (max 0 (min 1 alpha))]
            ;; TODO: rotate to particles
            (set! (.-alpha p) alpha)))))))

(defn- update-game-beta! []
  ;; game is alive?
 (when @my-cat-id
    (input/call-pressed-key-handler!))
 (let [
        blackhole-x (/ @p/screen-w 2) ; TODO
        blackhole-y (/ @p/screen-h 2) ; TODO: get from my-cat's logical-y
        ;;coins-data (:coins @event/test-queue)
        my-cat @event/my-cat
       ;;test (.log js/console (:theta my-cat))
        my-cat-angle (if (> (count my-cat) 0)
                       (:theta my-cat)
                       0)
       ;; exist-cats-num (mapv (fn [c] (get-cat-id c)) @event/cat-queue)
       other-cat-queue (filter #(not (:me %)) @event/cat-queue)]
   ;; update my cat
   (reset! my-cat-id (get-cat-id my-cat))
   (set! (.-x @geo-layer) blackhole-x)
   (set! (.-y @geo-layer) blackhole-y)
   (set! (.-angle @geo-layer) (* my-cat-angle -1))
   ;; remove old cats
   (doseq [k (map (fn [c] [(get-cat-id c) (:id c)]) other-cat-queue)]
     (swap! cats-idle-count assoc k 0))
   (doseq [k (keys @cats-idle-count)]
     (swap! cats-idle-count update-in [k] inc))
   (doseq [[[cindex cid] v] @cats-idle-count]
     (when (> v timeout-idle-count)
       (.kill (:sprite (get @cat-assets cindex)))
       (gstatus/update-status-window! {:id cid
                                       :me false
                                       :score 0
                                       :life 0
                                       :energy 0
                                       :moving "left"
                                       :img (str "cat" cindex)
                                       :jump false
                                       :theta 0
                                       :radius 0
                                       :timestamp 0})))
   (swap! cats-idle-count dissoc
          (keys (filter (fn [[k v]] (> v timeout-idle-count)) @cats-idle-count)))
   ;; update other cats
   (doseq [c @event/cat-queue]
     (update-cat! c my-cat-angle blackhole-x blackhole-y))
   (event/clear-cat-queue!)
   ;; update particles
   (update-particles!)
   ;; check game-over
   (when-not (pos? (:life my-cat))
     (emit-game-over! my-cat))
   nil
   ))

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
  (cond
    @preparing? (update-preparation!)
    @game-over? (update-game-over!)
    :else (update-game-beta!)))




(def state-map
  {:preload preload
   :create create
   :update update
   })
