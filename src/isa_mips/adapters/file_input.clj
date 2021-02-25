(ns isa-mips.adapters.file-input
  (:require [schema.core :as s]
            [isa-mips.models.action :as m.action]
            [isa-mips.helpers :as helpers]
            [clojure.string :as string]))

(s/defn action->action-type :- m.action/Type
  [action :- s/Str]
  (s/validate m.action/Type (keyword action)))

(s/defn ^:private little-endian-instruction
  [byte-array begin]
  (->> (+ begin 4)
       (subvec byte-array begin)
       (reverse)))

(s/defn byte-array->binary-instruction-array :- [s/Str]
  [byte-array :- [Byte]]
  (for [i (range 0 (count byte-array) 4)]
    (->> (little-endian-instruction byte-array i)
         (map helpers/binary-string)
         string/join)))
