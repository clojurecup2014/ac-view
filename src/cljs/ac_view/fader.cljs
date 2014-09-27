(ns ac-view.fader
  (:require-macros [ac-view.macros :as m]
                   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            [cljs.core.async :as async :refer [>! <!]]
            ))


(defn make! []
  (let [fader (p/add-sprite! :1x1  0 0 @p/screen-w @p/screen-h 0 0)]
    (set! (.-tint fader) 0)
    (set! (.-alpha fader) 0)
    (set! (.-z fader) 10000)
    (.kill fader)
    (set! (.---faded? fader) false)
    fader))

(defn cancel! [fobj]
  (when fobj
    (set! (.---faded? fobj) false)
    (.kill fobj)))

(defn fading? [fobj]
  (when fobj
    (.---faded? fobj)))

(def ^:constant interval 50) ; msec
(def ^:constant fade-time 800) ; msec

(defn fade! [fobj start end & [handle]]
  (when fobj
    (cancel! fobj)
    (set! (.-alpha fobj) start)
    (.revive fobj)
    (.bringToTop fobj)
    (set! (.---faded? fobj) true)
    (let [diff (- end start)
          begin (js/Date.now)]
      (go-loop []
        (<! (async/timeout interval))
        (let [elapsed (- (js/Date.now) begin)
              new-alpha (min 1 (max 0 (+ start (* diff (/ elapsed fade-time)))))]
          (set! (.-alpha fobj) new-alpha)
          (if (< fade-time elapsed)
            (when handle
              (<! (async/timeout interval))
              (handle)
              (set! (.---faded? fobj) false))
            (recur)))))))


