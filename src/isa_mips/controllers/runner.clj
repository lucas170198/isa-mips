(ns isa-mips.controllers.runner
  (:require [schema.core :as s]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.logic.instructions :as l.instructions]
            [isa-mips.controllers.i-ops :as c.i-ops]
            [isa-mips.controllers.syscall :as c.syscall]))

(defmulti execute-instruction! "Return nil for success execution"
          (fn [instruction] (:format instruction)))

(s/defmethod execute-instruction! :R
  [{func        :funct
    destiny-reg :rd
    first-reg   :rs
    second-reg  :rt} :- m.instruction/RInstruction]
  (c.r-ops/execute! func destiny-reg first-reg second-reg))

(s/defmethod execute-instruction! :I
  [{op-code     :op
    destiny-reg :rt
    reg         :rs
    immediate   :immediate} :- m.instruction/IInstruction]
  (c.i-ops/execute! op-code destiny-reg reg immediate))

(s/defmethod execute-instruction! :SYSCALL
  [_]
  (c.syscall/execute!))

(defn run-current-instruction! []
  (-> @db.memory/pc
      (db.memory/read-value!)
      (l.instructions/decode-binary-instruction)
      (execute-instruction!)))

(s/defn run-program! []
  (while (not= (run-current-instruction!) -1)               ;Stops in the exit syscall
    (db.memory/inc-program-counter)))