(ns isa-mips.controllers.runner
  (:require [schema.core :as s]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.db.simulation-summary :as db.simulation-summary]
            [isa-mips.logic.instructions :as l.instructions]
            [isa-mips.controllers.i-ops :as c.i-ops]
            [isa-mips.controllers.syscall :as c.syscall]
            [isa-mips.controllers.j-ops :as c.j-ops]
            [isa-mips.controllers.fr-ops :as c.fr-ops]
            [isa-mips.protocols.storage-client :as p-storage]))

(defmulti execute-instruction! "Return nil for success execution"
          (fn [{format :format} _ _]
            (when (contains? #{:R :I :J :FR :FI} format)
              (db.simulation-summary/inc-instructions-summary format))
            format))

(s/defmethod execute-instruction! :R
  [instruction :- m.instruction/RInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (c.r-ops/execute! instruction storage coproc-storage))


(s/defmethod execute-instruction! :I
  [instruction :- m.instruction/IInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (c.i-ops/execute! instruction storage coproc-storage))

(s/defmethod execute-instruction! :J
  [instruction :- m.instruction/JInstruction
   storage :- p-storage/IStorageClient
   _]
  (c.j-ops/execute! instruction storage))


(s/defmethod execute-instruction! :SYSCALL
  [_ storage coproc-storage]
  (db.simulation-summary/inc-instructions-summary :R)
  (c.syscall/execute! storage coproc-storage))

(s/defmethod execute-instruction! :NOP [_ _ _]
  (db.simulation-summary/inc-instructions-summary :R))

(s/defmethod execute-instruction! :FR
  [instruction :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (c.fr-ops/execute! instruction storage coproc-storage))

(defn run-instruction!
  ([storage coproc-storage] (run-instruction! @db.registers/pc storage coproc-storage))
  ([addr storage coproc-storage]
   (-> addr
       (db.registers/read-reg-value! storage)
       (l.instructions/decode-binary-instruction)
       (execute-instruction! storage coproc-storage))))

(s/defn run-program!
  [storage coproc-storage]
  (while true                                               ;Stops in the exit syscall
    (run-instruction! storage coproc-storage)
    (when-let [jump-addr @db.registers/jump-addr]              ;Verifies if the last instruction was a jump one
      (run-instruction! (+ 4 @db.registers/pc) storage coproc-storage)                ;run slotted delay instruction
      (db.registers/set-program-counter! jump-addr)
      (db.registers/clear-jump-addr!))
    (db.registers/inc-program-counter!)))
