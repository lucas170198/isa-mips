(ns isa-mips.controllers.rodata-section
  (:require [isa-mips.db.registers :as db.registers]
            [isa-mips.adapters.number-base :as a.number-base]))

(def rodata-section-init 0x00800000)

(defn store-rodata-section!
  "Store the data section bytes, starting by the addr 0x10010000"
  [byte-file storage]
  (dotimes [n (count byte-file)]
    (db.registers/write-value! (+ rodata-section-init n) (a.number-base/binary-string-zero-extend (nth byte-file n)) storage)))
