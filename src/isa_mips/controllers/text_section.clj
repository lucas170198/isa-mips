(ns isa-mips.controllers.text-section
  (:require [isa-mips.db.registers :as db.memory]))

(def text-section-init 0x00400000)

(defn store-text-section!
  [byte-file storage]
  "Store the program starting by the the addr 0x00400000"
  (dotimes [n (count byte-file)]
    (db.memory/write-value! (+ text-section-init (* n 4)) (nth byte-file n) storage)))
