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


;;; TODO: move to another module
(defn blink? []
  (zero? (mod (js/Math.floor (js/Date.now) / 1000) 2)))



(def normal-color 0xFFFFFF)
(def player-color 0xFFFF00)
(def dead-color 0x7F7F7F)

;;; TODO: sort by score (it needs to allow access by id, dont idx)

;;; TODO: score-text using bitmap font


(def ^:private ^:const debug? true)


(def ^:private text-style {:font "10px monospace" :align "left"})



(def status-layer (atom nil))



(defn add-status-layer! []
  (reset! status-layer (-> @p/game .-add .group))
  nil)


(def status-windows-info (atom {})) ; {win-idx info-map, ...}



;;; TODO: Must need GC
(def cat-id->win-idx (atom {}))





(defn- update-life-or-energy! [sprites life]
  (case life
    0 (do
        (.play (nth sprites 0) "off")
        (.play (nth sprites 1) "off")
        (.play (nth sprites 2) "off"))
    1 (do
        (.play (nth sprites 0) "on")
        (.play (nth sprites 1) "off")
        (.play (nth sprites 2) "off"))
    2 (do
        (.play (nth sprites 0) "on")
        (.play (nth sprites 1) "on")
        (.play (nth sprites 2) "off"))
    3 (do
        (.play (nth sprites 0) "on")
        (.play (nth sprites 1) "on")
        (.play (nth sprites 2) "on"))))



(defn- sort-win-idx []
  ;; not-empty -> alive -> score -> id-order
  (sort (fn [win-idx-a win-idx-b]
          (let [info-a (get @status-windows-info win-idx-a)
                info-b (get @status-windows-info win-idx-a)
                latest-status-a (:latest-status info-a)
                latest-status-b (:latest-status info-b)]
            (cond
              (and
                (nil? latest-status-a)
                (nil? latest-status-b)) 0
              (nil? latest-status-a) -1
              (nil? latest-status-b) 1
              (and
                (zero? (:life latest-status-a))
                (zero? (:life latest-status-b))) 0
              (zero? (:life latest-status-a)) -1
              (zero? (:life latest-status-b)) 1
              (< (:score latest-status-a)
                 (:score latest-status-b)) -1
              (< (:score latest-status-b)
                 (:score latest-status-a)) 1
              :else (compare win-idx-b win-idx-a))))))

(defn- find-free-win-idx []
  (let [not-found-in? (fn [vs v]
                        (not (some #(= v %) vs)))]
    (some (fn [checking-win-idx]
            (when (not-found-in? (vals @cat-id->win-idx) checking-win-idx)
              checking-win-idx))
          (range gcommon/cat-num))))

(defn- determine-win-idx [cat-id]
  (if-let [win-idx (@cat-id->win-idx cat-id)]
    win-idx
    (if (< (count @cat-id->win-idx) gcommon/cat-num)
      (let [win-idx (find-free-win-idx)]
        (swap! cat-id->win-idx assoc cat-id win-idx)
        win-idx)
      (last (sort-win-idx)))))

(defn- determine-window-info [cat-id]
  (get @status-windows-info (determine-win-idx cat-id)))


(defn update-status-window! [{cat-id :id
                              isme? :isme
                              score :score
                              life :life
                              energy :energy
                              theta :theta ; for debug display
                              radius :radius ; for debug display
                              }]
  (let [life (max 0 (min 3 life))
        energy (max 0 (min 3 energy))
        info (determine-window-info cat-id)
        dead? (zero? life)
        tint-color (cond
                     dead? dead-color
                     isme? player-color
                     :else normal-color)
        ]
    ;; tinting (dead / isme / normal)
    (when tint-color
      (set! (.-tint (:frame-sprite info)) tint-color)
      (set! (.-tint (:index-sprite info)) tint-color))
    ;; apply debug-text
    (when debug?
      (let [t (str "id=" (or cat-id "?") "\n"
                   "theta=" (or theta "?") "\n"
                   "radius=" (or radius "?"))]
        (set! (.-text (:debug-text info)) t)))
    ;; apply score
    (set! (.-text (:score-text info)) (.slice (str "     " score) -6))
    ;; apply life
    (update-life-or-energy! (:heart-sprites info) life)
    ;; apply energy
    (update-life-or-energy! (:energy-sprites info) energy)
    ;; apply dead or alive
    (if dead?
      (.kill (:cat-sprite info))
      (.revive (:cat-sprite info)))
    nil))



(defn- add-test-run-input-handler! []
  (let [life (atom 3)
        en (atom 3)
        score (atom 0)]
    (ac-view.input/set-handler!
      :Z
      (fn []
        (when-not (@ac-view.input/previous-keys-state :Z)
          ;; DUMMY change value
          (swap! score #(+ 10 %))
          (if (zero? @en)
            (swap! life dec)
            (swap! en dec))
          (update-status-window! {:id 1
                                  :isme true
                                  :score @score
                                  :life @life
                                  :energy @en
                                  :theta "???"
                                  :radius "???"
                                  }))))))





(defn gen-status-window! [status-x status-y win-idx]
  (let [win-group (-> @p/game .-add .group)
        ;;
        frame-sp (p/add-sprite! :status-frame-other status-x status-y)
        _ (set! (.-tint frame-sp) dead-color)
        _ (.add win-group frame-sp)
        ;;
        cat-sp (gcommon/prepare-cat-sprite! win-idx)
        cat-x (- status-x 24)
        _ (set! (.-x cat-sp) cat-x)
        _ (set! (.-y cat-sp) status-y)
        _ (.add win-group cat-sp)
        _ (.kill cat-sp)
        ;;
        debug-text-x (- status-x 200)
        debug-text (when debug?
                     (p/add-text! "" debug-text-x (- status-y 20) text-style))
        _ (when debug?
            (.add win-group debug-text))
        ;;
        index-sp (asset/gen-numbers!) ; index number
        index-x (- status-x 70)
        _ (set! (.-x index-sp) index-x)
        _ (set! (.-y index-sp) status-y)
        _ (asset/set-numbers! index-sp (inc win-idx))
        _ (set! (.-tint index-sp) dead-color)
        _ (.add win-group index-sp)
        ;;
        heart-sps (doall
                    (map
                      (fn [i]
                        (let [sp (asset/gen-heart!)
                              item-x (+ status-x 6 (* 14 i))
                              item-y (+ status-y -10)]
                          (set! (.-x sp) item-x)
                          (set! (.-y sp) item-y)
                          (.play sp "off")
                          (.add win-group sp)
                          sp))
                      (range 3)))
        ;;
        energy-sps (doall
                     (map
                       (fn [i]
                         (let [sp (asset/gen-energy!)
                               item-x (+ status-x 6 (* 14 i))
                               item-y (+ status-y 1)]
                           (set! (.-x sp) item-x)
                           (set! (.-y sp) item-y)
                           (.play sp "off")
                           (.add win-group sp)
                           sp))
                       (range 3)))
        ;;
        score-text-x (+ status-x 10)
        score-text-y (+ status-y 10)
        score-text (p/add-text! "     0" score-text-x score-text-y text-style)
        _ (.add win-group score-text)
        ;;
        _ (.add @status-layer win-group)]
    {:win-idx win-idx
     :group win-group
     :frame-sprite frame-sp
     :cat-sprite cat-sp
     :debug-text debug-text
     :index-sprite index-sp
     :heart-sprites heart-sps
     :energy-sprites energy-sps
     :score-text score-text
     :latest-status (atom nil)
     }))





(defn prepare-status-layer-async! []
  (when debug?
    (add-test-run-input-handler!))
  (go
    (reset! status-windows-info {})
    (let [x 730
          y 40
          y-diff 56]
      (dotimes [i gcommon/cat-num]
        (<! (async/timeout 1))
        (let [win (gen-status-window! x (+ y (* i y-diff)) i)]
          (swap! status-windows-info assoc i win)))
      ;; Notice for prepare
      (swap! gcommon/prepared-set conj :status))))




