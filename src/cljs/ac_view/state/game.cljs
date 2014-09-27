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
            ))

(def fader (atom nil))


(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)


(defn create [& _]
  (reset! fader (fader/make!))
  (asset/add-bg!)
  (fader/fade! @fader 1 0 #(js/alert "TODO"))
  nil)


(defn update [& _]
  nil)





(def state-map
  {:preload preload
   :create create
   :update update
   })
