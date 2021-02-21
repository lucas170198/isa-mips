(ns isa-mips.helpers
  (:require [schema.core :as s]))

(s/defn hex->num :- s/Int
  [s :- s/Str]
  (Integer/parseInt (.substring s 2) 16))
