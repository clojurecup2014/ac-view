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
(def menu-game-off (atom nil))
(def menu-game-on (atom nil))
(def menu-ranking (atom nil))
(def menu-rule (atom nil))
(def menu-start (atom nil))

(def title-logo (atom nil))

(defn preload [& _]
  nil)

(defn create [& _]
  (asset/add-bg!)
  (let [screen-w @p/screen-w
        screen-h @p/screen-h
        screen-w-half (/ screen-w 2)
        screen-h-half (/ screen-h 2)
        ]
    (reset! hole (p/add-sprite! :hole screen-w-half screen-h-half))

    (reset! title-logo (p/add-text! "TITLE LOGO" screen-w-half 200 {:font "32px monospace"}))

    ;; TODO: work
    (reset! menu-frame (p/add-sprite! :menu-frame screen-w-half 400))
    (reset! menu-start (p/add-sprite! :menu-game-start 250 400))
    (reset! menu-rule (p/add-sprite! :menu-game-rule 350 400))
    (reset! menu-ranking (p/add-sprite! :menu-game-ranking 450 400))
    (reset! menu-game-off (p/add-sprite! :menu-game-off 550 400))
    (reset! menu-game-on (p/add-sprite! :menu-game-on 550 450))
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
