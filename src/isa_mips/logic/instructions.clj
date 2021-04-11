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

(s/defn ^:private I-format-instruction
  [binary-string]
  {:format    :I
   :op        (subs binary-string 0 6)
   :rs        (subs binary-string 6 11)
   :rt        (subs binary-string 11 16)
   :immediate (subs binary-string 16 32)})

(s/defn ^:private J-format-instruction
  [binary-string]
  {:format         :J
   :op             (subs binary-string 0 6)
   :target-address (subs binary-string 6 32)})

(s/defn ^:private FR-format-instruction
  [binary-string]
  {:format :FR
   :op     (subs binary-string 0 6)
   :fmt    (subs binary-string 6 11)
   :ft     (subs binary-string 11 16)
   :fs     (subs binary-string 16 21)
   :fd     (subs binary-string 21 26)
   :funct  (subs binary-string 26 32)})

(s/defn decode-binary-instruction :- m.instruction/BaseInstruction
  [binary-string]
  ;(println binary-string)
  (cond
    (= (Integer/parseUnsignedInt binary-string 2) 0x0000000C)
    {:format :SYSCALL}

    (= (Integer/parseUnsignedInt binary-string 2) 0x00000000)
    {:format :NOP}

    (= (subs binary-string 0 6) "000000")
    (R-format-instruction binary-string)

    (= (subs binary-string 0 6) "010001")
    (FR-format-instruction binary-string)

    (= (subs binary-string 0 3) "001")
    (I-format-instruction binary-string)


    (= (subs binary-string 0 3) "100")
    (I-format-instruction binary-string)

    (= (subs binary-string 0 3) "101")
    (I-format-instruction binary-string)

    (= (subs binary-string 0 3) "110")
    (I-format-instruction binary-string)

    (= (subs binary-string 0 4) "0001")
    (I-format-instruction binary-string)

    (= (subs binary-string 0 5) "00001")
    (J-format-instruction binary-string)

    :else
    (throw (Exception. (str "Op code not found: " binary-string)))))
