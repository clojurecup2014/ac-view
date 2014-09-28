(ns ac-view.state.title
  (:require-macros [ac-view.macros :as m]
                   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [crate.core :as c]
            [crate.util :as cutil]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            [ac-view.asset :as asset]
            [ac-view.fader :as fader]
            [ac-view.input :as input]
            [cljs.core.async :as async :refer [>! <!]]
            ))


(def vote-url "https://clojurecup.com/#/apps/astrocats")


(def hole (atom nil))

(def menu-frame (atom nil))
(def menu-sound-off (atom nil))
(def menu-sound-on (atom nil))
(def menu-ranking (atom nil))
(def menu-rule (atom nil))
(def menu-start (atom nil))

(def title-logo (atom nil))

(def fader (atom nil))

(def menu-keys
  [:menu-start :menu-rule :menu-member #_:menu-ranking :menu-sound-off :menu-sound-on])
(defn menu-left [k]
  ({:menu-start :menu-start
    :menu-rule :menu-start
    :menu-member :menu-rule
    ;:menu-ranking :menu-member
    :menu-sound-off :menu-member
    :menu-sound-on :menu-member
    } k))
(defn menu-right [k]
  ({:menu-start :menu-rule
    :menu-rule :menu-member
    :menu-member (if @asset/disable-sound? :menu-sound-off :menu-sound-on)
    ;:menu-ranking (if @asset/disable-sound? :menu-sound-off :menu-sound-on)
    :menu-sound-off :menu-sound-off
    :menu-sound-on :menu-sound-on
    } k))

(def menu-objs (atom nil))
(def selected (atom nil))

(defn preload [& _]
  (p/disable-visibility-change! true)
  nil)

(defn- reset-with-info! [atm & kvs]
  (reset! atm (apply hash-map kvs)))

(defn- add-button-animation! [obj]
  (try
    (-> obj .-animations (.add "off" (array 0) 1 false))
    (-> obj .-animations (.add "on" (array 1) 1 false))
    (catch :default e nil))
  (.play obj "off"))

(defn- button-select! [k]
  (dorun (map #(.play (get @menu-objs %) "off") menu-keys))
  (.play (get @menu-objs k) "on")
  (reset! selected k))

(defn- do-vote! []
  (js/window.open vote-url "_blank"))

(defn- do-tweet! []
  (let [tweet-url (asset/get-tweet-url (str "This is "
                                            "test."))]
    (js/window.open tweet-url "_blank")))


(defn create [& _]
  (asset/add-bg!)
  (let [screen-w @p/screen-w
        screen-h @p/screen-h
        screen-w-half (/ screen-w 2)
        screen-h-half (/ screen-h 2)
        menu-y 480
        sound-x 580
        gen-sprite-button! (fn [k x y]
                             (doto (p/add-sprite! k x y)
                               (add-button-animation!)))
        description-y 380
        ]
    (reset! menu-objs
            (into
              {}
              [[:hole (p/add-sprite! :hole screen-w-half screen-h-half)]
               [:title-logo (p/add-sprite! :title-logo screen-w-half 180)]
               [:menu-frame (gen-sprite-button! :menu-frame screen-w-half menu-y)]
               [:menu-start (gen-sprite-button! :menu-game-start 230 menu-y)]
               [:menu-rule (gen-sprite-button! :menu-game-rule 365 menu-y)]
               [:menu-member (gen-sprite-button! :menu-game-member 470 menu-y)]
               ;[:menu-ranking (gen-sprite-button! :menu-game-ranking 515 menu-y)]
               [:menu-sound-off (gen-sprite-button! :menu-sound-off sound-x menu-y)]
               [:menu-sound-on (gen-sprite-button! :menu-sound-on sound-x menu-y)]
               ]))
    (if @asset/disable-sound?
      (.kill (:menu-sound-on @menu-objs))
      (.kill (:menu-sound-off @menu-objs)))

    ;(p/add-text! "KEYBOARD-Z: submit, jump" 150 description-y)
    ;(p/add-text! "CURSOR-LEFT and RIGHT: move" 450 description-y)

    (p/add-sprite! :howto 400 description-y)

    (input/add-key-capture!)
    (button-select! :menu-start)

    (let [v-x (+ 400 -62 -100)
          v-y 540]
      (-> @p/game .-add (.button v-x v-y "menu-game-vote" do-vote! nil 1 0)))
    (let [v-x (+ 400 -62 100)
          v-y 540]
      (-> @p/game .-add (.button v-x v-y "menu-game-tweet" do-tweet! nil 1 0)))

    nil))

(defn- go-state! [k]
  (when-not @fader
    (reset! fader (fader/make!)))
  (fader/fade! @fader 0 1 #(p/start-state! k)))

(defn- go-game! []
  (when @input/ws-emitter
    (@input/ws-emitter))
  (go-state! :game))

(def display-mode (atom nil))

(def rule (atom nil))
(defn- show-rule! []
  (reset! display-mode :rule)
  (reset! rule (p/add-sprite! :rule 400 250))
  nil)
(defn- hide-rule! []
  (reset! display-mode nil)
  (.destroy @rule)
  (reset! rule nil)
  nil)

(def member (atom nil))
(defn- show-member! []
  (reset! display-mode :member)
  (reset! member (p/add-sprite! :member 400 250))
  nil)
(defn- hide-member! []
  (reset! display-mode nil)
  (.destroy @member)
  (reset! member nil)
  nil)

(defn- activate-button! [k]
  (case k
    :menu-start (go-game!)
    :menu-rule (show-rule!)
    :menu-member (show-member!)
    :menu-ranking (js/alert "not implemented yet") ; TODO
    :menu-sound-off (do
                      (asset/enable-sound!)
                      (.revive (:menu-sound-on @menu-objs))
                      (.kill (:menu-sound-off @menu-objs))
                      (button-select! :menu-sound-on))
    :menu-sound-on (do
                      (asset/disable-sound!)
                      (.revive (:menu-sound-off @menu-objs))
                      (.kill (:menu-sound-on @menu-objs))
                      (button-select! :menu-sound-off))
    ))

(defn update [& _]
  (when-not (fader/fading? @fader)
    (when-let [k (input/get-just-pressed-key)]
      (case @display-mode
        :rule (hide-rule!)
        :member (hide-member!)
        ;; else
        (cond
          (= :Z k) (activate-button! @selected)
          (= :L k) (button-select! (menu-left @selected))
          (= :R k) (button-select! (menu-right @selected))
          ))
      (asset/beep!))))






(def state-map
  {:preload preload
   :create create
   :update update
   })
