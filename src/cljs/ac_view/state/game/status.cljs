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


(def ^:private ^:const debug? false)


;;; TODO: score-text using bitmap font


;;; TODO: move to another module
(defn blink? []
  (zero? (mod (js/Math.floor (js/Date.now) / 1000) 2)))



(def normal-color 0xFFFFFF)
(def player-color 0xFFFF00)
(def dead-color 0x7F7F7F)


(def ^:private client-timeout-msec (* 5 60 1000))


(def ^:private x-offset 730)
(def ^:private y-offset 40)
(def ^:private y-diff 56)

(def ^:private life-max 3)
(def ^:private energy-max 5)



(def ^:private text-style {:font "10px monospace" :align "left"})



(def status-layer (atom nil))





(defn add-status-layer! []
  (reset! status-layer (-> @p/game .-add .group))
  nil)


(def status-windows-info (atom {})) ; {win-idx info-map, ...}



(def cat-sprite-pool (atom {})) ; {img-num sprite, ...}



(def cat-id->win-idx (atom {}))





(defn- update-life-or-energy! [sprites value number]
  (dotimes [i number]
    (if (< i value)
      (.play (nth sprites i) "on")
      (.play (nth sprites i) "off"))))



(defn- sort-win-idx []
  ;; not-empty -> alive -> score -> id-order
  (sort (fn [win-idx-a win-idx-b]
          (let [info-a (get @status-windows-info win-idx-a)
                info-b (get @status-windows-info win-idx-b)
                latest-status-a @(:latest-status info-a)
                latest-status-b @(:latest-status info-b)]
            (cond
              ;(and
              ;  (nil? latest-status-a)
              ;  (nil? latest-status-b)) 0
              ;(and
              ;  (not (nil? latest-status-a))
              ;  (nil? latest-status-b)) -1
              ;(and
              ;  (nil? latest-status-a)
              ;  (not (nil? latest-status-b))) 1
              (and
                (pos? (:life latest-status-a))
                (zero? (:life latest-status-b))) -1
              (and
                (zero? (:life latest-status-a))
                (pos? (:life latest-status-b))) 1
              (< (:score latest-status-b)
                 (:score latest-status-a)) -1
              (< (:score latest-status-a)
                 (:score latest-status-b)) 1
              :else (compare win-idx-a win-idx-b))))
        (range (count @status-windows-info))))

(defn- find-free-win-idx []
  (let [not-found-in? (fn [vs v]
                        (not (some #(= v %) vs)))]
    (some (fn [checking-win-idx]
            (when (not-found-in? (vals @cat-id->win-idx) checking-win-idx)
              checking-win-idx))
          (range gcommon/cat-num))))

(defn- spawn-sfx! [win-idx]
  (let [sp (:blink-sprite (get @status-windows-info win-idx))]
    (asset/play-se! :beep)
    (set! (.-alpha sp) 0.8)
    (-> @p/game .-add (.tween sp) (.to (js-obj "alpha" 0) 500 nil) (.start))))

(defn- search-most-obsoleted-win-idx []
  (first
    (sort-by #(let [info (get @status-windows-info %)
                    latest-status @(:latest-status info)]
                (or (:timestamp latest-status) 0))
             (range gcommon/cat-num))))

(defn- determine-win-idx [m]
  (let [cat-id (:id m)]
    (if-let [win-idx (@cat-id->win-idx cat-id)]
      ;; Exist
      win-idx
      ;; Not exist
      (let [i (if (< (count @cat-id->win-idx) gcommon/cat-num)
                ;; use empty slot
                (let [win-idx (find-free-win-idx)]
                  (swap! cat-id->win-idx assoc cat-id win-idx)
                  win-idx)
                ;; reuse old slot
                (let [candidate (last (sort-win-idx))
                      candidate-info (get @status-windows-info candidate)
                      latest-status @(:latest-status candidate-info)]
                  (if-not (pos? (:life latest-status))
                    candidate
                    (search-most-obsoleted-win-idx))))
            info (get @status-windows-info i)
            cat-image-number (:img m)
            cat-sp (get @cat-sprite-pool cat-image-number)
            ]
        (when cat-sp
          ((:cat-locator info) cat-sp))
        (reset! (:cat-sprite info) cat-sp)
        (spawn-sfx! i)
        i))))

(defn- determine-window-info [m]
  (get @status-windows-info (determine-win-idx m)))


(defn- resolve-server-parameter [m]
  ;; {:damaged false
  ;;  :type "cat"
  ;;  :energy 5
  ;;  :theta -20.000
  ;;  :radius 149.99...
  ;;  :life 3
  ;;  :vx 0
  ;;  :vy 0
  ;;  :id "G__4009"
  ;;  :score 0
  ;;  :moving "left"
  ;;  :me true
  ;;  :jump false
  ;;  :img "cat9"
  ;;  }
  (let [life (max 0 (min life-max (:life m 0)))
        energy (max 0 (min energy-max (:energy m 0)))
        timestamp (or (:timestamp m) (js/Date.now))
        img (:img m 0)
        img (cond
              (number? img) img
              (string? img) (let [[_ img-number] (re-find #"^cat(\d+)$" img)]
                              (or (js/parseInt img-number 10) 0))
              :else 0)
        ]
    (merge m {:life life
              :energy energy
              :timestamp timestamp
              :img img
              })))

(defn update-status-window! [m & [force-win-idx]]
  (let [m (resolve-server-parameter m)
        {cat-id :id
         is-me? :me
         score :score
         life :life
         energy :energy
         moving :moving
         img :img
         jump? :jump
         theta :theta ; for debug display
         radius :radius ; for debug display
         timestamp :timestamp ; for internal use
         } m
        info (if force-win-idx
               (get @status-windows-info force-win-idx)
               (determine-window-info m))
        dead? (zero? life)
        tint-color (cond
                     dead? dead-color
                     is-me? player-color
                     :else normal-color)]
    ;; kill old cat sprite when changing sprite
    (when force-win-idx
      (when-let [sp @(:cat-sprite info)]
        (reset! (:cat-sprite info) nil)
        (.kill sp)))
    ;; update :latest-status
    (reset! (:latest-status info) m)
    ;; tinting (dead / isme / normal)
    (when tint-color
      (set! (.-tint (:frame-sprite info)) tint-color)
      (set! (.-tint (:index-sprite info)) tint-color))
    ;; apply debug-text
    (when debug?
      (let [t (str "id=" (or cat-id "?") "\n"
                   "moving=" moving "\n"
                   "jump=" jump? "\n"
                   )]
        (set! (.-text (:debug-text info)) t)))
    ;; apply score
    (set! (.-text (:score-text info)) (.slice (str "     " score) -6))
    ;; apply life
    (update-life-or-energy! (:heart-sprites info) life life-max)
    ;; apply energy
    (update-life-or-energy! (:energy-sprites info) energy energy-max)
    ;; set new cat sprite when changing sprite
    (when (and force-win-idx (not dead?))
      (let [cat-image-number (:img m)
            cat-sp (get @cat-sprite-pool cat-image-number)]
        (when cat-sp
          ((:cat-locator info) cat-sp))
        (reset! (:cat-sprite info) cat-sp)))
    ;; apply dead or alive
    (when-let [cat-sp @(:cat-sprite info)]
      (if dead?
        (do
          (.kill cat-sp)
          (reset! (:cat-sprite info) nil))
        (.revive cat-sp)))
    nil))



(defn- determine-empty-img []
  (let [used-img (set
                   (map
                     (fn [info]
                       (let [latest-status @(:latest-status info)
                             img (:img latest-status)
                             life (:life latest-status 0)]
                         (when (pos? life)
                           img)))
                     (vals @status-windows-info)))]
  (some (fn [i]
          (if (used-img i)
            nil
            i))
        (range gcommon/cat-num))))


(defn- add-test-run-input-handler! []
  (let [;life (atom 3)
        ;en (atom 3)
        ;score (atom 0)
        idx (atom 0)
        ]
    (ac-view.input/set-handler!
      :Z
      (fn []
        (when-not (@ac-view.input/previous-keys-state :Z)
          ;; DUMMY change value
          ;(swap! score #(+ 10 %))
          ;(if (zero? @en)
          ;  (swap! life dec)
          ;  (swap! en dec))
          ;(update-status-window! {:id 1
          ;                        :isme true
          ;                        :score @score
          ;                        :life @life
          ;                        :energy @en
          ;                        :moving nil
          ;                        :img nil
          ;                        :jump nil
          ;                        :theta "???"
          ;                        :radius "???"
          ;                        })
          (swap! idx inc)
          (update-status-window! {:id @idx
                                  :isme (rand-nth [true false])
                                  :score (rand-int 100)
                                  :life (rand-int (inc life-max))
                                  :energy (rand-int (inc energy-max))
                                  :moving nil
                                  ;:img (rand-int gcommon/cat-num)
                                  :img (determine-empty-img)
                                  :jump nil
                                  :theta (rand-int 360)
                                  :radius (rand-int 300)
                                  })
          nil)))))



(defn- sort-status-windows! [new-cat-id->win-idx
                             order
                             new-win-idx-order
                             old-status-windows-info
                             previous-ms]
  (let [doit! (fn []
                (doseq [i order]
                  (let [old-idx i
                        new-idx (nth new-win-idx-order i)
                        old-info (get old-status-windows-info old-idx)
                        new-m (nth previous-ms new-idx)
                        ]
                    ;; update status slot
                    (update-status-window! new-m old-idx)))
                (reset! cat-id->win-idx new-cat-id->win-idx)
                ;; refresh cat sprite life/death
                (doseq [i order]
                  (let [info (get @status-windows-info i)
                        cat-sp @(:cat-sprite info)
                        latest-status @(:latest-status info)
                        life (:life latest-status 0)]
                    (when cat-sp
                      (if (pos? life)
                        (.revive cat-sp)
                          (reset! (:cat-sprite info) nil)
                          (.kill cat-sp))))))
        sp @status-layer
        half-msec 200
        from-point 1
        middle-point 0
        to-point 1
        _ (set! (.-alpha sp) from-point)
        ;_ (.revive sp)
        ;; setup t2
        t2 (-> @p/game .-add (.tween sp))
        _ (.to t2 (js-obj "alpha" to-point) half-msec)
        _ (.add (.-onComplete t2) (fn [& _] nil))
        ;; setup t1
        t1 (-> @p/game .-add (.tween sp))
        _ (.to t1 (js-obj "alpha" middle-point) half-msec)
        _ (.add (.-onComplete t1) (fn [& _]
                                    (doit!)
                                    (.start t2)))
        ]
    (.start t1)))


(def ^:private watcher-is-running? (atom nil))
(defn- run-watcher! []
  (when-not @watcher-is-running?
    (let [order (range gcommon/cat-num)]
      (reset! watcher-is-running? true)
      (go-loop []
        (<! (async/timeout 5000))
        ;; Remove living timeout cats
        (let [now (js/Date.now)]
          (doseq [i order]
            (let [info (get @status-windows-info i)
                  latest-status @(:latest-status info)
                  timestamp (:timestamp latest-status)]
              (when (and
                      (pos? (:life latest-status 0))
                      (< (+ timestamp client-timeout-msec) now))
                (let [cleared-status {:id (:id info)
                                      :isme false
                                      :score 0
                                      :life 0
                                      :energy 0
                                      :moving nil
                                      :img ""
                                      :jump nil
                                      :theta 0
                                      :radius 0
                                      }
                      cat-id (:id latest-status)]
                  (update-status-window! cleared-status i)
                  (swap! cat-id->win-idx dissoc cat-id))))))
        ;; Sort cats
        (let [old-status-windows-info @status-windows-info
              ;; NB: It needs for update with sideefects
              previous-ms (map (fn [i]
                                 @(:latest-status
                                    (get old-status-windows-info i)))
                               order)
              new-win-idx-order (sort-win-idx)
              new-cat-id->win-idx (into {}
                                        (map (fn [[k v]]
                                               [k (nth new-win-idx-order v)])
                                             @cat-id->win-idx))]
          (when-not (= order new-win-idx-order)
            (sort-status-windows! new-cat-id->win-idx
                                  order
                                  new-win-idx-order
                                  old-status-windows-info
                                  previous-ms))
          (recur))))))







(defn gen-status-window! [status-x status-y win-idx]
  (let [win-group (-> @p/game .-add .group)
        ;;
        frame-sp (p/add-sprite! :status-frame-other status-x status-y)
        _ (set! (.-tint frame-sp) dead-color)
        _ (.add win-group frame-sp)
        ;;
        cat-x (- status-x 24)
        ;cat-sp (gcommon/prepare-cat-sprite! win-idx)
        ;_ (set! (.-x cat-sp) cat-x)
        ;_ (set! (.-y cat-sp) status-y)
        ;_ (.add win-group cat-sp)
        ;_ (.kill cat-sp)
        cat-locator (fn [sp]
                      (when sp
                        (set! (.-x sp) cat-x)
                        (set! (.-y sp) status-y)
                        (.bringToTop sp)
                        (.revive sp)))
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
                      (range life-max)))
        ;;
        energy-sps (doall
                     (map
                       (fn [i]
                         (let [sp (asset/gen-energy!)
                               item-x (+ status-x 3 (* 9 i))
                               item-y (+ status-y 2)]
                           (set! (.-x sp) item-x)
                           (set! (.-y sp) item-y)
                           (.play sp "off")
                           (.add win-group sp)
                           sp))
                       (range energy-max)))
        ;;
        score-text-x (+ status-x 10)
        score-text-y (+ status-y 10)
        score-text (p/add-text! "     0" score-text-x score-text-y text-style)
        _ (.add win-group score-text)
        ;;
        blink-sp (p/add-sprite! :1x1 status-x status-y)
        _ (set! (.-width blink-sp) (.-width frame-sp))
        _ (set! (.-height blink-sp) (.-height frame-sp))
        _ (.add win-group blink-sp)
        _ (set! (.-alpha blink-sp) 0)
        ;;
        _ (.add @status-layer win-group)]
    {:win-idx win-idx
     :group win-group
     :frame-sprite frame-sp
     :cat-locator cat-locator
     :cat-sprite (atom nil)
     :debug-text debug-text
     :index-sprite index-sp
     :heart-sprites heart-sps
     :energy-sprites energy-sps
     :blink-sprite blink-sp
     :score-text score-text
     :latest-status (atom {:id 0
                           :isme false
                           :score 0
                           :life 0
                           :energy 0
                           :moving nil
                           :img (str "cat" win-idx)
                           :jump nil
                           :theta 0
                           :radius 0
                           })
     }))




(defn prepare-status-layer-async! []
  ;(when debug?
  ;  (add-test-run-input-handler!))
  (go
    (reset! status-windows-info {})
    (reset! cat-sprite-pool {})
    (dotimes [i gcommon/cat-num]
      (<! (async/timeout 1))
      (let [sp (gcommon/prepare-cat-sprite! i)]
        (.kill sp)
        (.add @status-layer sp)
        (swap! cat-sprite-pool assoc i sp)))
    (dotimes [i gcommon/cat-num]
      (<! (async/timeout 1))
      (let [win (gen-status-window! x-offset (+ y-offset (* i y-diff)) i)]
        (swap! status-windows-info assoc i win)))
    ;; Run watcher
    (run-watcher!)
    ;; Notice for prepare
    (swap! gcommon/prepared-set conj :status)))

(defn get-previous-frame-status [cat-id]
  (let [win-idx (@cat-id->win-idx cat-id)
        info (get @status-windows-info win-idx)
        latest-status (:latest-status info)]
    (when latest-status
      @latest-status)))
