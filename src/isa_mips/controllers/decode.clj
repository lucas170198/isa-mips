(ns isa-mips.controllers.decode
  (:require [schema.core :as s]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.controllers.i-ops :as c.i-ops]
            [isa-mips.controllers.j-ops :as c.j-ops]
            [isa-mips.controllers.fr-ops :as c.fr-ops]))

(defmulti instruction-string! (fn [instruction] (:format instruction)))

(defmethod instruction-string! :SYSCALL
  [_instruction]
  "syscall")

(defmethod instruction-string! :NOP
  [_instruction]
  "nop")

(s/defmethod instruction-string! :R
  [{func :funct
    destiny-reg :rd
    first-reg :rs
    second-reg :rt
    shamt :shamt} :- m.instruction/RInstruction]
  (c.r-ops/operation-str! func destiny-reg first-reg second-reg shamt))

(s/defmethod instruction-string! :FR
  [{func :funct
    fd :fd
    fs :fs
    ft :ft}]
  (c.fr-ops/operation-str! func fd fs ft))

(s/defmethod instruction-string! :I
  [{op-code :op
    destiny-reg :rt
    reg :rs
    immediate :immediate} :- m.instruction/IInstruction]
  (c.i-ops/operation-str! op-code destiny-reg reg immediate))

(s/defmethod instruction-string! :J
  [{op-code :op
    addr :target-address} :- m.instruction/JInstruction]
  (c.j-ops/operation-str! op-code addr))

(s/defn print-instructions!
  [instructions :- [m.instruction/BaseInstruction]]
  (doseq [inst instructions]
    (println (instruction-string! inst))))
