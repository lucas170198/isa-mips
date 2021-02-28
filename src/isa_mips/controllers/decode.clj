(ns isa-mips.controllers.decode
  (:require [schema.core :as s]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.controllers.i-ops :as c.i-ops]))

(defmulti instruction-string! (fn [instruction] (:format instruction)))

(defmethod instruction-string! :SYSCALL
  [_instruction]
  "syscall")

(s/defmethod instruction-string! :R
  [{func :funct
    destiny-reg :rd
    first-reg :rs
    second-reg :rt} :- m.instruction/RInstruction]
  (c.r-ops/operation-str! func destiny-reg first-reg second-reg))

(s/defmethod instruction-string! :I
  [{op-code :op
    destiny-reg :rt
    reg :rs
    immediate :immediate} :- m.instruction/IInstruction]
  (c.i-ops/operation-str! op-code destiny-reg reg immediate))

(s/defn print-instructions!
  [instructions :- [m.instruction/BaseInstruction]]
  (doseq [inst instructions]
    (println (instruction-string! inst))))
