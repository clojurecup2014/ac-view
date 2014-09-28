(ns ac-view.input
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            ))

(def previous-keys-state (atom #{}))
(def keys-state (atom #{}))

(def key-handlers (atom {}))


(defn add-key-capture! []
  (p/add-key-capture! :LEFT :RIGHT :Z))


(defn update-key-state! []
  (reset! previous-keys-state @keys-state)
  (reset! keys-state #{})
  (when (p/is-key-down? :LEFT)
    (swap! keys-state conj :L))
  (when (p/is-key-down? :RIGHT)
    (swap! keys-state conj :R))
  (when (p/is-key-down? :Z)
    (swap! keys-state conj :Z)))


;;; for title and other
(defn get-just-pressed-key []
  (let [prev-keys @keys-state]
    (update-key-state!)
    (cond
      (and (not (prev-keys :Z)) (@keys-state :Z)) :Z
      (and (not (prev-keys :L)) (@keys-state :L) (not (@keys-state :R))) :L
      (and (not (prev-keys :R)) (@keys-state :R) (not (@keys-state :L))) :R
      :else nil)))

;;; for game (call key-handlers)
(defn call-pressed-key-handler! []
  (update-key-state!)
  (dorun (map #(when-let [h (get @key-handlers %)]
                 (when (get @keys-state %)
                   (h)))
              [:Z :L :R])))


(defn set-handler! [k handle]
  (swap! key-handlers assoc k handle))


