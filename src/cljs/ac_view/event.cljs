(ns ac-view.event
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            ))


(def test-queue (atom nil))

(defn enqueue! [event]
  ;; This is test implementation
  (swap! test-queue #(take 10 (cons event %))))



