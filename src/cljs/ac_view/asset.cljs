(ns ac-view.asset
  (:require-macros [ac-view.macros :as m])
  (:require [crate.core :as c]
            [domina :as d]
            [domina.events :as ev]
            [phaser-cljs.core :as p]
            ))

;;; TODO: Separate Audio module

(defn get-master-vol []
  0.5)

(def disable-sound? (atom nil))
(defn enable-sound! []
  ;; TODO: Replay bgm
  (reset! disable-sound? false)
  (js/localStorage.setItem "disable-sound" "0"))
(defn disable-sound! []
  ;; TODO: Stop bgm
  (reset! disable-sound? true)
  (js/localStorage.setItem "disable-sound" "1"))
(defn toggle-sound! []
  (if @disable-sound?
    (enable-sound!)
    (disable-sound!)))

(def sound-keys (atom #{}))

(defn- register-se! [k & files]
  (apply p/load-audio! k files)
  (swap! sound-keys conj k))

(defn load-all-assets! []
  (p/load-image! :bg "bg.jpg")
  (p/load-image! :1x1 "1x1.png")
  (p/load-image! :hole "hole.png")
  ;(p/load-spritesheet! :hoge "hoge.png" frame-w frame-h & [frame-max margin spacing])

  ;; title
  (p/load-image! :menu-frame "top/menu_frame.png")
  (p/load-spritesheet! :menu-sound-off "top/menu_game_off.png" 75 26)
  (p/load-spritesheet! :menu-sound-on "top/menu_game_on.png" 72 26)
  (p/load-spritesheet! :menu-game-ranking "top/menu_game_ranking.png" 103 26)
  (p/load-spritesheet! :menu-game-rule "top/menu_game_rule.png" 68 26)
  (p/load-spritesheet! :menu-game-start "top/menu_game_start.png" 138 26)
  (p/load-image! :title-logo "top/title.png")

  ;; sounds
  (register-se! :beep "beep.ogg" "beep.mp3")

  nil)


(def sound-objs (atom {}))



(defn register-all-sounds! []
  ;; load from localStorage
  (reset! disable-sound?  (= "1" (js/localStorage.getItem "disable-sound")))
  (dorun (map #(swap! sound-objs assoc % (-> @p/game .-add (.audio (name %))))
              @sound-keys)))



(defn add-bg! []
  (let [x (/ @p/screen-w 2)
        y (/ @p/screen-h 2)
        w @p/screen-w
        h @p/screen-h
        bg (p/add-sprite! :bg x y w h)]
    bg))

(defn play-se! [k]
  (when-not @disable-sound?
    (.play (get @sound-objs k) "" 0 (get-master-vol))))

(defn beep! []
  (play-se! :beep))

