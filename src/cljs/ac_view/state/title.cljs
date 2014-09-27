(ns ac-view.state.title
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            ))


(defn preload [& _]
  nil)

(defn create [& _]
  (asset/add-bg!)
  ;; TODO: Add more assets
  ;(js/alert "this is title")
  nil)

(defn update [& _]
  ;; TODO
  nil)

(def state-map
  {:preload preload
   :create create
   :update update
   })
