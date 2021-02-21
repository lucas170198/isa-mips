(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [isa-mips.models.memory :as m.memory]))

(defn ^:private register-class
  [prefix init-idx vector]
  (into {} (map-indexed (fn [idx _]
                          {(+ idx init-idx) {:name  (str prefix idx)
                                             :value 0}}) vector)))

(def text-section-init 4194304)

(def data-section-init 268500992)

(def ^:private pointers
  {28 {:name "$gp" :value 4194304}
   29 {:name "$sp" :value 2147479548}
   30 {:name "$fp" :value 0}
   31 {:name "$ra" :value 0}})

(def pc (atom text-section-init))

(def mem
  (->> (merge {0 {:name "$zero" :value 0}
               1 {:name "$at" :value 0}}
              (register-class "$v" 2 (range 2))
              (register-class "$a" 4 (range 4))
              (register-class "$t" 8 (range 8))
              (register-class "$s" 16 (range 8))
              (register-class "$t" 24 (range 8 9))
              (register-class "$k" 26 (range 2))
              pointers)
       (s/validate m.memory/Store)
       (atom)))

(s/defn read-value! :- s/Int
  [address :- s/Int]
  (:value (get @mem address)))

(s/defn write-value!
  [address :- s/Int
   value :- s/Int]
  (swap! mem assoc-in [address :value] value))
