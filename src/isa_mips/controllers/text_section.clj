(ns isa-mips.controllers.text-section
  (:require [isa-mips.db.memory :as db.memory]
            [isa-mips.logic.text-section :as l.text-section]))


(defn store-text-section!
  [byte-file storage]
  "Store the program starting by the the addr 0x00400000"
  (dotimes [n (count byte-file)]
    (db.memory/write-instruction! (l.text-section/instruction-address n) (nth byte-file n) storage)))
