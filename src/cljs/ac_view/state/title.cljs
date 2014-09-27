(ns ac-view.state.title
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            ))


(def hole (atom nil))

(def menu-frame (atom nil))
(def menu-sound-off (atom nil))
(def menu-sound-on (atom nil))
(def menu-ranking (atom nil))
(def menu-rule (atom nil))
(def menu-start (atom nil))

(def title-logo (atom nil))

(def menu-keys
  [:menu-start :menu-rule :menu-ranking :menu-sound-off :menu-sound-on])
(def menu-objs (atom nil))
(def selected (atom nil))

(defn preload [& _]
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
  (p/disable-visibility-change! true)
  (asset/add-bg!)
  (let [screen-w @p/screen-w
        screen-h @p/screen-h
        screen-w-half (/ screen-w 2)
        screen-h-half (/ screen-h 2)
        menu-y 500
        sound-x 550
        gen-sprite-button! (fn [k x y]
                             (doto (p/add-sprite! k x y)
                               (add-button-animation!)))
        ]
    (reset! menu-objs
            {:hole (gen-sprite-button! :hole screen-w-half screen-h-half)
             :title-logo (gen-sprite-button! :title-logo screen-w-half 200)
             :menu-frame (gen-sprite-button! :menu-frame screen-w-half menu-y)
             :menu-start (gen-sprite-button! :menu-game-start 250 menu-y)
             :menu-rule (gen-sprite-button! :menu-game-rule 350 menu-y)
             :menu-ranking (gen-sprite-button! :menu-game-ranking 450 menu-y)
             :menu-sound-off (gen-sprite-button! :menu-sound-off sound-x menu-y)
             :menu-sound-on (gen-sprite-button! :menu-sound-on sound-x menu-y)
             })
    (.kill (:menu-sound-off @menu-objs))

    (button-select! :menu-start)

    nil))

(defn update [& _]
  ;(p/debug-text! "test" 100 100)
  ;; TODO
  nil)

(def state-map
  {:preload preload
   :create create
   :update update
   })
