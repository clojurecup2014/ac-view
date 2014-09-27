(ns ac-view.state.game.common
  (:require-macros [ac-view.macros :as m]
                   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [cljs.core.async :as async :refer [>! <!]]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            [ac-view.fader :as fader]
            [ac-view.input :as input]
            [ac-view.event :as event]
            ))


(def block-size 32)

(def cat-num 10)

(def coin-num 10)

(def fader (atom nil))

(def prepared-set (atom #{}))

;(defn- get-cat-color [i]
;  (nth [0xFFFFFF
;        0xFFFF7F
;        0xFF7FFF
;        0xFF7F7F
;        0x7FFFFF
;        0x7FFF7F
;        0x7F7FFF
;        0x7F7F7F
;        0x3F3F3F
;        0xBFBFBF] i 0xFFFFFF))

(defn prepare-cat-sprite! [i]
  (let [sp (asset/gen-cat! :cat0)
        ;color (get-cat-color i)
        ]
    ;(set! (.-tint sp) color) ; NB: tinting kill animation!
    sp))


