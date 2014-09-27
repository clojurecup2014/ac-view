(ns ac-view.state
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.state.bootstrap]
            ))

(defn add-all! []
  ;; TODO: Reconsider interface
  (p/add-state! :bootstrap ac-view.state.bootstrap/state-map)
  nil)
