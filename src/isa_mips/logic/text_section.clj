(ns isa-mips.logic.text-section
  (:require [schema.core :as s]))

(s/def ^:private text-section-init 0x00400000)

(s/defn instruction-address
  [offset]
  (+ text-section-init (* offset 4)))
