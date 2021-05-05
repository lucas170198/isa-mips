(ns isa-mips.db.stats
  (:require [schema.core :as s]
            [isa-mips.models.memory :as models.memory]))


(defn ^:private update-if
  "Update a element that matches with the pred"
  [coll fn key pred-fn]
  (mapv #(if (pred-fn %)
           (update % key fn) %) coll))

(s/def instructions-summary (atom {:R 0 :I 0 :J 0 :FR 0 :FI 0}))

(s/def mem-summary (atom [{:level :RAM :Hits 0 :Misses 0}]))

(defn ^:private find-by-level
  [level]
  (filterv #(= (:level %) level) @mem-summary))

(defn ^:private inc-memory-stat
  [level stat]
  (reset! mem-summary (update-if @mem-summary inc stat #(= (:level %) level))))

(s/defn hit-memory-by-level!
  [level :- models.memory/level-type]
  (if (empty? (find-by-level level))
    (swap! mem-summary conj {:level level :Hits 1 :Misses 0})
    (inc-memory-stat level :Hits)))

(s/defn miss-memory-by-level!
  [level :- models.memory/level-type]
  (if (empty? (find-by-level level))
    (swap! mem-summary conj {:level level :Hits 0 :Misses 1})
    (inc-memory-stat level :Misses)))

(s/def start-execution (atom (System/currentTimeMillis)))

(s/defn millis-since-started
  []
  (- (System/currentTimeMillis) @start-execution))

(s/defn inc-instructions-summary
  [type :- s/Keyword]
  (swap! instructions-summary #(update % type inc)))

