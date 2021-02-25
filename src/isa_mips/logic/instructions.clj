(ns isa-mips.logic.instructions
  (:require [schema.core :as s]
            [isa-mips.models.instruction :as m.instruction]))

(s/defn ^:private R-format-instruction
  [binary-string]
  {:format :R
   :op     (subs binary-string 0 6)
   :rs     (subs binary-string 6 11)
   :rt     (subs binary-string 11 16)
   :rd     (subs binary-string 16 21)
   :shamt  (subs binary-string 21 26)
   :funct  (subs binary-string 26 32)})

(s/defn decode-binary-instruction :- m.instruction/BaseInstruction
  [binary-string]
  (case (subs binary-string 0 6)
    "000000" (R-format-instruction binary-string)
    "001000" :I))
