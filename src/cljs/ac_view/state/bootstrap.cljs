(ns ac-view.state.bootstrap
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            ))


(defn preload [& _]
  (p/disable-visibility-change!)
  ;; Refit screen size
  (p/set-resize-handler!)
  ;; Loading assets
  (asset/load-all-assets!)
  nil)

(defn create [& _]
  (asset/add-bg!)
  nil)

(defn update [& _]
  (p/start-state! :title)
  ;; TODO
  nil)

(def state-map
  {:preload preload
   :create create
   :update update
   })
