(ns isa-mips.controllers.runner
  (:require [schema.core :as s]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.db.stats :as db.stats]
            [isa-mips.logic.instructions :as l.instructions]
            [isa-mips.controllers.i-ops :as c.i-ops]
            [isa-mips.controllers.syscall :as c.syscall]
            [isa-mips.controllers.j-ops :as c.j-ops]
            [isa-mips.controllers.fr-ops :as c.fr-ops]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.db.memory :as db.memory]))

(defmulti execute-instruction! "Return nil for success execution"
          (fn [{format :format} _ _ _]
            (when (contains? #{:R :I :J :FR :FI} format)
              (db.stats/inc-instructions-summary format))
            format))

(s/defmethod execute-instruction! :R
  [instruction :- m.instruction/RInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   _]
  (c.r-ops/execute! instruction storage coproc-storage))


(s/defmethod execute-instruction! :I
  [instruction :- m.instruction/IInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   memory :- p-storage/IStorageClient]
  (c.i-ops/execute! instruction storage coproc-storage memory))

(s/defmethod execute-instruction! :J
  [instruction :- m.instruction/JInstruction
   storage :- p-storage/IStorageClient
   _
   _ :- p-storage/IStorageClient]
  (c.j-ops/execute! instruction storage))


(s/defmethod execute-instruction! :SYSCALL
  [_ storage coproc-storage memory]
  (db.stats/inc-instructions-summary :R)
  (c.syscall/execute! storage coproc-storage memory))

(s/defmethod execute-instruction! :NOP [_ _ _ _]
  (db.stats/inc-instructions-summary :R))

(s/defmethod execute-instruction! :FR
  [instruction :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   _]
  (c.fr-ops/execute! instruction storage coproc-storage))

(defn run-instruction!
  ([storage coproc-storage memory] (run-instruction! @db.registers/pc storage coproc-storage memory))
  ([addr registers coproc-storage memory]
   (-> addr
       (db.memory/read-instruction! memory)
       (l.instructions/decode-binary-instruction)
       (execute-instruction! registers coproc-storage memory))))

(s/defn run-program!
  [storage coproc-storage memory]
  (while true                                               ;Stops in the exit syscall
    (run-instruction! storage coproc-storage memory)
    (when-let [jump-addr @db.registers/jump-addr]              ;Verifies if the last instruction was a jump one
      (run-instruction! (+ 4 @db.registers/pc) storage coproc-storage memory)                ;run slotted delay instruction
      (db.registers/set-program-counter! jump-addr)
      (db.registers/clear-jump-addr!))
    (db.registers/inc-program-counter!)))
