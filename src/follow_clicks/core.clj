(ns follow-clicks.core
  (:gen-class)
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.test :refer [is]]))

;; ============================
;; Constants:
(def width 500)
(def height 500)
(def background "resources/background.png")
(def player-image "resources/player.png")
(def player-speed 5)
(def distance-size 24)
(def target-tolerance 3)

;; ============================
;; Data definitions:

(defrecord Player [pos-x pos-y target-x target-y distance])
;; Player is (Player. Number[0, width] Number[0, height] Number)
;; interp. a player with position, a target to move to and the distance traveled so far
;;         - everything in pixel units
;;         - pos-x, pos-y are the position in x,y coordinates
;;         - target-x, target-y are the target position in x,y coordinates
(def p1 (Player.   0 0   0 0   0))    ; start player, not yet moved
(def p2 (Player. 100 0 100 0 100))    ; reached target after 100px
(def p3 (Player.  12 5  19 4 312))    ; moving towards target

(defn fn-for-player [p]
  (... (:pos-x p)       ; Number[0, width]
       (:pos-x p)       ; Number[0, height]
       (:pos-x p)       ; Number[0, width]
       (:pos-x p)       ; Number[0, height]
       (:pos-x p)))     ; Number

;; Template rules used:
;;  - compound: 5 fields

;; ============================
;; Functions:

(declare move-player)
(declare render)
(declare handle-mouse)
(declare target-distance)

;; Player -> Player
;; start the world with (main (Player. 0 0 0 0 0))
(defn main [player]
  (q/sketch :title "follows click"
            :size   [width height]
            :setup  (fn [] player)        ; _ -> Player
            :update move-player           ; Player -> Player
            :draw   render                ; Player -> nil
            :mouse-clicked handle-mouse   ; Player MouseEvent -> Player
            :middleware [m/fun-mode m/pause-on-error])
  player)

;; _ -> Player
;; start the world with the default player, used for CLI calls
(defn -main
  [& _]
  (main (Player. 0 0 0 0 0)))

;; Player -> Player
;; move player by player-speed towards target and record player-speed as travel distance
(is (= (move-player (Player. 0 5 100 5 50))                              ; horizontal movement
       (Player. (+ 0 player-speed) 5 100 5 (+ 50 player-speed))))
(is (= (move-player (Player. 10 5 10 50 70))                             ; vertical movement
       (Player. 10 (+ 5 player-speed) 10 50 (+ 70 player-speed))))
(is (= (move-player p3)                                                  ; oblique movement
       (Player. (+ (:pos-x p3)
                   (* player-speed
                      (/ (- (:target-x p3) (:pos-x p3))
                         (target-distance p3))))
                (+ (:pos-y p3)
                   (* player-speed
                      (/ (- (:target-y p3) (:pos-y p3))
                         (target-distance p3))))
                (:target-x p3) (:target-y p3)
                (+ (:distance p3) player-speed))))
(is  (= (move-player (Player. 101 42 (+ 101 target-tolerance) 42 0))   ; target within tolerance
        (Player. 101 42 (+ 101 target-tolerance) 42 0)))

;; (defn move-player [player] p1) ; stub

;; < use template from Player >
(defn move-player [player]
  (if (<= (target-distance player) target-tolerance)
    player
    (Player. (+ (:pos-x player)
                (* player-speed
                   (/ (- (:target-x player) (:pos-x player))
                      (target-distance player))))
             (+ (:pos-y player)
                (* player-speed
                   (/ (- (:target-y player) (:pos-y player))
                      (target-distance player))))
             (:target-x player) (:target-y player)
             (+ (:distance player) player-speed))))

;; Player -> nil
;; render player character on its position and total distance traveled above the player
;; !!! tests missing, quil rendering only has side effects, no return value

(defn render [player]
  (q/image (q/load-image background) 0 0)
  (let [player-image (q/load-image player-image)]
    (q/with-translation [(/ (.width player-image) -2) (/ (.height player-image) -2)]
      (q/image player-image (:pos-x player) (:pos-y player))
      (q/text-align :center)
      (q/text-size distance-size)
      (q/fill 0)
      (q/text (str (:distance player))
              (+ (:pos-x player) (/ (.width player-image) 2))
              (:pos-y player)))))

;; Player MouseEvent -> Player
;; om mouse click, set the player target to the position of the click
(is (= (handle-mouse p3 {:x 13 :y 74 :button :left})
       (Player. (:pos-x p3) (:pos-y p3) 13 74 (:distance p3))))

(defn handle-mouse [player event]
  (cond (= (:button event) :left)
        (Player. (:pos-x player) (:pos-y player) (:x event) (:y event) (:distance player))
        :else player))

;; Player -> Integer
;; produce the distance from player to its target
(is (= (target-distance (Player. 0 0 10 0 0)) 10))
(is (= (target-distance (Player. 0 0  1 1 0))  1))
(is (= (target-distance (Player. 1 3  5 2 0))
       (Math/round (Math/sqrt (+ (Math/pow (- 5 1) 2) (Math/pow (- 2 3) 2))))))

;; (define (target-distance player) 0) ; stub

;; < use template from Player >
(defn target-distance [p]
  (Math/round (Math/sqrt (+ (Math/pow (- (:target-x p) (:pos-x p)) 2)
                            (Math/pow (- (:target-y p) (:pos-y p)) 2)))))
