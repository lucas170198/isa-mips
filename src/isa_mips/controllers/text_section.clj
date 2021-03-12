(ns isa-mips.controllers.text-section
  (:require [isa-mips.db.memory :as db.memory]))

(def text-section-init 0x00400000)

(defn store-text-section!
  [byte-file]
  "Store the program starting by the the addr 0x00400000"
  (dotimes [n (count byte-file)]
    (db.memory/write-value! (+ text-section-init (* n 4)) (nth byte-file n))))

(defn integer-reg-value!
  [reg]
  (-> (Long/parseLong reg 2)
      (db.memory/read-value!)
      (Long/parseLong 2)))
