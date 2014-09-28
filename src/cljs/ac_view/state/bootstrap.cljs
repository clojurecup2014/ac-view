(ns ac-view.state.bootstrap
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            ))


(defn preload [& _]
  (p/disable-visibility-change! true)
  ;; Refit screen size
  (p/set-resize-handler!)
  ;; Loading assets
  (asset/load-loading-assets!)
  nil)

(defn create [& _]
  (asset/add-bg!)
  (p/add-text! "NOW LOADING" 350 300)
  (asset/load-all-assets!
    (fn [& _]
      (asset/register-all-sounds!)
      (p/start-state! :title))))

(defn update [& _]
  nil)

(def state-map
  {:preload preload
   :create create
   :update update
   })
