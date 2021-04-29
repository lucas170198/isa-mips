(ns isa-mips.controllers.decode
  (:require [schema.core :as s]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.controllers.r-ops :as c.r-ops]
            [isa-mips.controllers.i-ops :as c.i-ops]
            [isa-mips.controllers.j-ops :as c.j-ops]
            [isa-mips.controllers.fr-ops :as c.fr-ops]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.logic.text-section :as l.text-section]
            [isa-mips.logic.binary :as l.binary]))

(defmulti instruction-string! (fn [instruction _ _] (:format instruction)))

(defmethod instruction-string! :SYSCALL
  [_ _ _]
  "syscall")

(defmethod instruction-string! :NOP
  [_ _ _]
  "nop")

(s/defmethod instruction-string! :R
  [instruction :- m.instruction/RInstruction
   storage :- p-storage/IStorageClient
   _]
  (c.r-ops/operation-str! instruction storage))

(s/defmethod instruction-string! :FR
  [instruction :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (c.fr-ops/operation-str! instruction storage coproc-storage))

(s/defmethod instruction-string! :I
  [instruction :- m.instruction/IInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (c.i-ops/operation-str! instruction storage coproc-storage))

(s/defmethod instruction-string! :J
  [instruction :- m.instruction/JInstruction
   _
   _]
  (c.j-ops/operation-str! instruction))

(s/defn ^:private print-instruction-table!
  [instructions]
  (clojure.pprint/print-table [:address :hex :decoded] instructions))

(s/defn ^:private hex-address
  [offset]
  (-> (l.text-section/instruction-address offset)
      Integer/toBinaryString
      l.binary/bin->hex-str))

(s/defn print-instructions!
  [storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   instructions :- [m.instruction/BaseInstruction]]
  (-> (fn [idx inst] {:address (hex-address idx)
                      :hex     (:hex inst)
                      :decoded (instruction-string! inst storage coproc-storage)})
      (map-indexed instructions)
      print-instruction-table!))
