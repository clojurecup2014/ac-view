(ns ac-view.state
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.state.bootstrap]
            [ac-view.state.title]
            [ac-view.state.game]
            ))

(defn add-all! []
  ;; TODO: Reconsider interface
  (p/add-state! :bootstrap ac-view.state.bootstrap/state-map)
  (p/add-state! :title ac-view.state.title/state-map)
  (p/add-state! :game ac-view.state.game/state-map)
  nil)
