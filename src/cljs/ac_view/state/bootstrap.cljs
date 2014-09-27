(ns ac-view.state.bootstrap
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            ))


(defn preload [& _]
  ;; Refit screen size
  (p/set-resize-handler!)
  ;; Loading assets
  (p/load-audio! :beep "beep.ogg" "beep.mp3")
  (p/load-image! :bg "bg.jpg")
  (p/load-image! :1x1 "1x1.png")
  (p/load-image! :hole "hole.png")
  ;(p/load-spritesheet! :hoge "hoge.png" frame-w frame-h & [frame-max margin spacing])
  nil)

(defn create [& _]
  ;; Test for display :bg
  (let [x (/ @p/screen-w 2)
        y (/ @p/screen-h 2)
        w @p/screen-w
        h @p/screen-h
        bg (p/add-sprite! :bg x y w h)]
    bg)
  ;(js/alert :ok)
  nil)

(defn update [& _]
  ;; TODO
  nil)

(def state-map
  {:preload preload
   :create create
   :update update
   })
