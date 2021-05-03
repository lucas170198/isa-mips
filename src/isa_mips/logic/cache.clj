(ns isa-mips.logic.cache
  (:require [schema.core :as s]
            [isa-mips.models.memory :as m.memory]))


(s/def ^:private config-table
  [{:levels 0 :met []}
   {:levels 1 :meta [{:type :unified :size 1024 :assoc-param 0 :line-size 32}]}
   {:levels 1 :meta [{:type :split :size 1024 :assoc-param 0 :line-size 32}]}])

(s/defn configuration :- m.memory/CacheConfig
  [op-mode :- s/Int]
  (nth config-table op-mode {:levels 0 :met []}))




