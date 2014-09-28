(ns ac-view.state.title
  (:require-macros [ac-view.macros :as m]
                   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            [ac-view.fader :as fader]
            [ac-view.input :as input]
            [cljs.core.async :as async :refer [>! <!]]
            ))


(def hole (atom nil))

(def menu-frame (atom nil))
(def menu-sound-off (atom nil))
(def menu-sound-on (atom nil))
(def menu-ranking (atom nil))
(def menu-rule (atom nil))
(def menu-start (atom nil))

(def title-logo (atom nil))

(def fader (atom nil))

(def menu-keys
  [:menu-start :menu-rule :menu-ranking :menu-sound-off :menu-sound-on])
(defn menu-left [k]
  ({:menu-start :menu-start
    :menu-rule :menu-start
    :menu-ranking :menu-rule
    :menu-sound-off :menu-ranking
    :menu-sound-on :menu-ranking
    } k))
(defn menu-right [k]
  ({:menu-start :menu-rule
    :menu-rule :menu-ranking
    :menu-ranking (if @asset/disable-sound? :menu-sound-off :menu-sound-on)
    :menu-sound-off :menu-sound-off
    :menu-sound-on :menu-sound-on
    } k))

(def menu-objs (atom nil))
(def selected (atom nil))

(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)

(defn- reset-with-info! [atm & kvs]
  (reset! atm (apply hash-map kvs)))

(defn- add-button-animation! [obj]
  (-> obj .-animations (.add "off" (array 0) 1 false))
  (-> obj .-animations (.add "on" (array 1) 1 false))
  (.play obj "off"))

(defn- button-select! [k]
  (dorun (map #(.play (get @menu-objs %) "off") menu-keys))
  (.play (get @menu-objs k) "on")
  (reset! selected k))

(defn create [& _]
  (asset/add-bg!)
  (let [screen-w @p/screen-w
        screen-h @p/screen-h
        screen-w-half (/ screen-w 2)
        screen-h-half (/ screen-h 2)
        menu-y 500
        sound-x 580
        gen-sprite-button! (fn [k x y]
                             (doto (p/add-sprite! k x y)
                               (add-button-animation!)))
        ]
    (reset! menu-objs
            {:hole (p/add-sprite! :hole screen-w-half screen-h-half)
             :title-logo (p/add-sprite! :title-logo screen-w-half 200)
             :menu-frame (gen-sprite-button! :menu-frame screen-w-half menu-y)
             :menu-start (gen-sprite-button! :menu-game-start 250 menu-y)
             :menu-rule (gen-sprite-button! :menu-game-rule 380 menu-y)
             :menu-ranking (gen-sprite-button! :menu-game-ranking 480 menu-y)
             :menu-sound-off (gen-sprite-button! :menu-sound-off sound-x menu-y)
             :menu-sound-on (gen-sprite-button! :menu-sound-on sound-x menu-y)
             })
    (if @asset/disable-sound?
      (.kill (:menu-sound-on @menu-objs))
      (.kill (:menu-sound-off @menu-objs)))

    (p/add-text! "KEYBOARD-Z: select, jump" 100 400)
    (p/add-text! "CURSOR-LEFT and RIGHT: move" 400 400)

    (input/add-key-capture!)
    (button-select! :menu-start)

    nil))

(defn- go-state! [k]
  (when-not @fader
    (reset! fader (fader/make!)))
  (fader/fade! @fader 0 1 #(p/start-state! k)))

(def display-mode (atom nil))
(def rule (atom nil))
(defn- show-rule! []
  (reset! display-mode :rule)
  (reset! rule (p/add-sprite! :rule 400 300))
  nil)

(defn- hide-rule! []
  (reset! display-mode nil)
  (.destroy @rule)
  (reset! rule nil)
  nil)

(defn- activate-button! [k]
  (case k
    :menu-start (go-state! :game)
    :menu-rule (show-rule!)
    :menu-ranking (js/alert "not implemented yet") ; TODO
    :menu-sound-off (do
                      (asset/enable-sound!)
                      (.revive (:menu-sound-on @menu-objs))
                      (.kill (:menu-sound-off @menu-objs))
                      (button-select! :menu-sound-on))
    :menu-sound-on (do
                      (asset/disable-sound!)
                      (.revive (:menu-sound-off @menu-objs))
                      (.kill (:menu-sound-on @menu-objs))
                      (button-select! :menu-sound-off))
    ))

(defn update [& _]
  (when-not (fader/fading? @fader)
    (when-let [k (input/get-just-pressed-key)]
      (case @display-mode
        :rule (hide-rule!)
        ;; else
        (cond
          (= :Z k) (activate-button! @selected)
          (= :L k) (button-select! (menu-left @selected))
          (= :R k) (button-select! (menu-right @selected))
          ))
      (asset/beep!))))






(def state-map
  {:preload preload
   :create create
   :update update
   })
