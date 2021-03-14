(ns isa-mips.controllers.data-section
  (:require [isa-mips.db.memory :as db.memory]
            [isa-mips.adapters.number-base :as a.number-base]))

(def data-section-init 0x10010000)

(defn store-data-section!
  "Store the data section bytes, starting by the addr 0x10010000"
  [byte-file]
  (dotimes [n (count byte-file)]
    (db.memory/write-value! (+ data-section-init n) (a.number-base/binary-string-zero-extend (nth byte-file n)))))
