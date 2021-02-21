(ns isa-mips.models.memory
  (:require [schema.core :as s])
  (:import [clojure.lang PersistentHashMap]))

(def hash-map? ^:private
  (fn ^:static hash-map? [x] (instance? PersistentHashMap x)))

(def data {(s/required-key :value) s/Int
           :name                   s/Str})

(def store-skeleton {s/Int data})

(def Store (s/constrained store-skeleton hash-map?))
