(ns isa-mips.db.simulation-summary
  (:require [schema.core :as s]))


(s/def instructions-count (atom {:R 0 :I 0 :J 0 :FR 0 :FI 0}))

(s/def start-execution (atom (System/currentTimeMillis)))

(s/defn millis-since-started
  []
  (- (System/currentTimeMillis) @start-execution))

(s/defn inc-instructions-count
  [type :- s/Keyword]
  (swap! instructions-count #(update % type inc)))