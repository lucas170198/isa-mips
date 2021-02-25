(ns isa-mips.controllers.text-section
  (:require [schema.core :as s]
            [isa-mips.models.instruction :as m.instruction]
            [clojure.string :as string]
            [isa-mips.helpers :as helpers]))

(s/defn little-endian-array
  [text-section]
  (for [i (range 0 (count text-section) 4)]
    (->> (+ i 4)
         (subvec text-section i)
         (reverse)
         (map helpers/binary-string)
         string/join)))

(s/defn decode
  [text-section]
  (->> (subvec text-section 8 11)
       (map #(Integer/toHexString %))
       (string/join)))