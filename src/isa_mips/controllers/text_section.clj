(ns isa-mips.controllers.text-section
  (:require [schema.core :as s]))

(s/defn binary-array-to-instructions :-  [s/Str]
  [binary-array]
  (let [byte-num (count binary-array)]
    ))