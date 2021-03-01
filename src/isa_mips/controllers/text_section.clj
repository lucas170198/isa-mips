(ns isa-mips.controllers.text-section
  (:require [isa-mips.db.memory :as db.memory]))

(def text-section-init 0x00400000)

(defn store-text-section!
  [byte-file]
  "Store the program starting by the the addr 0x00400000"
  (dotimes [n (count byte-file)]
    (db.memory/write-value! (+ text-section-init n) (nth byte-file n))))
