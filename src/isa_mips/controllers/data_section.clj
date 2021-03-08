(ns isa-mips.controllers.data-section
  (:require [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(def data-section-init 0x10010000)

(defn ^:private remove-end-blank-lines
  [byte-file]
  (take-while #(not (zero? %)) byte-file))

(defn store-data-section!
  "Store the data section bytes, starting by the addr 0x10010000"
  [byte-file]
  (dotimes [n (count byte-file)]
    (db.memory/write-value! (+ data-section-init n) (helpers/binary-string (nth byte-file n)))))
