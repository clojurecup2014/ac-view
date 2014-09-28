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

  ;; game
  (p/load-spritesheet! :numbers "game/number.png" (/ 174 6) (/ 116 4))
  ;; 0 1 2 3 4 5
  ;; 0 1 2 3 4 5
  ;; 6 7 8 9 10
  ;; 6 7 8 9 10
  (p/load-image! :status-frame-me "game/status_frame_me.png")
  (p/load-image! :status-frame-other "game/status_frame_other.png")
  (p/load-spritesheet! :status-item "game/status_item.png" (/ 30 2) (/ 30 2))
  ;; 有ハート 有エネルギーゲージ
  ;; 空ハート 有エネルギーゲージ
  (p/load-spritesheet! :cat0 "game/driftcat0.png" (/ 128 4) (/ 128 4))
  (p/load-image! :coin "game/coin.png")
  (p/load-image! :step "game/step.png")

  nil)


(defn get-numbers-index [number & [altered?]]
  (if (and (integer? number) (<= 0 number 10))
    (let [i (nth [0 1 2 3 4 5 12 13 14 15 16] number)]
      (if altered? (+ i 6) i))
    17)) ; empty

(defn gen-numbers! []
  (let [sp (p/add-sprite! :numbers 0 0)]
    (dotimes [i (* 4 6)]
      (-> sp .-animations (.add (str i) (array i) 1 false)))
    sp))

(defn set-numbers! [sp & [number]]
  (.play sp (get-numbers-index number)))


(defn gen-cat! [key-or-idx]
  (let [k (if (keyword? key-or-idx)
            key-or-idx
            (keyword (str "cat" key-or-idx)))
        c (p/add-sprite! k 0 0)]
    (-> c .-animations (.add "stay" (array 0 1 2 3) 2 true))
    (-> c .-animations (.add "walk" (array 4 5 6) 10 true))
    (-> c .-animations (.add "sitting" (array 8 10) 2 false))
    (-> c .-animations (.add "sit" (array 10) 1 false))
    (.play c "stay")
    c))

(defn gen-heart! []
  (let [sp (p/add-sprite! :status-item 0 0)]
    (-> sp .-animations (.add "on" (array 0) 1 false))
    (-> sp .-animations (.add "off" (array 2) 1 false))
    sp))

(defn gen-energy! []
  (let [sp (p/add-sprite! :status-item 0 0)]
    (-> sp .-animations (.add "on" (array 1) 1 false))
    (-> sp .-animations (.add "off" (array 3) 1 false))
    sp))



(def sound-objs (atom {}))



(defn register-all-sounds! []
  ;; load from localStorage
  (reset! disable-sound?  (= "1" (js/localStorage.getItem "disable-sound")))
  (dorun (map #(swap! sound-objs assoc % (p/add-audio! %))
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

