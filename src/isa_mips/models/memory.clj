(ns isa-mips.models.memory
  (:require [schema.core :as s]))

(def data {:value                 s/Str
           (s/optional-key :name) s/Str})

(def store-skeleton {:addr s/Int :meta data})

(def Store [store-skeleton])
