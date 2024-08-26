(ns test-main.rpg
  (:require [odoyle.rules :as o]))

(def game-state (atom {:status :ongoing}))

(def rules
  (o/ruleset
   {::player
    [:what
     [?id ::type :player]
     [?id ::health ?health]
     :then
     (println "Player found:" ?id "with health:" ?health)
     (swap! game-state assoc :player-health ?health)]

    ::enemy
    [:what
     [?id ::type :enemy]
     [?id ::health ?health]
     :then
     (println "Enemy found:" ?id "with health:" ?health)
     (swap! game-state assoc :enemy-health ?health)]

    ::attack
    [:what
     [?attacker ::type ?attacker-type]
     [?attacker ::health ?attacker-health]
     [?target ::type ?target-type]
     [?target ::health ?target-health]
     :when
     (and (not= ?attacker-type ?target-type)
          (> ?attacker-health 0)
          (> ?target-health 0))
     :then
     (let [damage (+ 5 (rand-int 11))
           new-health (max 0 (- ?target-health damage))]
       (println ?attacker-type "attacks" ?target-type "for" damage "damage")
       (o/insert! ?target ::health new-health))]

    ::check-game-over
    [:what
     [::player-1 ::health ?player-health]
     [::enemy-1 ::health ?enemy-health]
     :when
     (or (<= ?player-health 0) (<= ?enemy-health 0))
     :then
     (swap! game-state assoc :status :over)
     (cond
       (and
       (<= ?player-health 0) (<= ?enemy-health 0)) (swap! game-state assoc :winner :tie)
       (<= ?player-health 0) (swap! game-state assoc :winner :enemy)
       (<= ?enemy-health 0) (swap! game-state assoc :winner :player))]}))

(defn create-session []
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::player-1 ::type :player)
      (o/insert ::player-1 ::health 100)
      (o/insert ::enemy-1 ::type :enemy)
      (o/insert ::enemy-1 ::health 100)
      (o/fire-rules)))

(defn game-loop [session]
  (let [new-session (o/fire-rules session)]
    (if (= (:status @game-state) :over)
      (do
        (println "Game over!")
        (case (:winner @game-state)
          :player (println "Player wins!")
          :enemy (println "Enemy wins!")
          :tie (println "It's a tie!"))
        (println "Final health - Player:" (:player-health @game-state)
                 "Enemy:" (:enemy-health @game-state)))
      (do
        (println "---")
        (recur new-session)))))

(defn -main []
  (println "Starting new game!")
  (reset! game-state {:status :ongoing})
  (game-loop (create-session)))

(-main)
