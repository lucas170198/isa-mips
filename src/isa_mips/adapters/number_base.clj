(ns isa-mips.adapters.number-base
  (:require [schema.core :as s]))

(s/defn bin->numeric
  [bin :- s/Str]
  (Long/parseLong bin 2))
