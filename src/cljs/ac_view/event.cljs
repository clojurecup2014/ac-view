(ns ac-view.event
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]))

(def my-cat (atom nil))

(def global-map (atom nil))
(def global-blocks (atom nil))

(def cat-queue (atom []))

(defn- parse-json
  [data]
  (js->clj (.parse js/JSON data)
           :keywordize-keys true))

(defn enqueue! [event-data]
  (let [e (parse-json event-data)
        t (:type e)]
    (case t
      "cat" (do (swap! cat-queue #(vec (take 20 (cons e %))))
                (when (:me e)
                  (reset! my-cat e)))
      "coin" nil
      "blocks" (reset! global-blocks (:blocks e))
      "map" (reset! global-map e)
      nil)))

(defn clear-cat-queue! []
  (reset! cat-queue []))
