(ns ac-view.event
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]))

(def cat-queue (atom []))

(defn enqueue! [event]
  (let [e (js->clj (.parse js/JSON event) :keywordize-keys true)]
    (let [t (:type e)]
      (case t
        "cat" (swap! cat-queue #(vec (take 10 (cons e %))))
        "coin" nil
        "block" nil
        nil))))

(defn clear-cat-queue! []
  (reset! cat-queue []))
