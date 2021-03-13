(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]
            [isa-mips.adapters.number-base :as a.number-base]))

(defn exit! []
  (println)
  (System/exit 0))

(defn print-int!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      a.number-base/bin->numeric
      print))

(s/defn ^:private printable-array! []
  (loop [addr  (a.number-base/bin->numeric (db.memory/read-value-by-name! "$a0"))
         array '()]
    (let [character (a.number-base/bin->numeric (db.memory/read-value! addr))]
      (if (= character 0)
        array
        (recur (inc addr)
               (concat array (list (char character))))))))

(defn print-string!
  []
  (->> (printable-array!)
       (apply str)
       println))

(defn print-char!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      a.number-base/bin->numeric
      char
      print))

(defn read-integer!
  []
  (let [input-value (Integer/parseInt (read-line))]
    (db.memory/write-value! 2 (helpers/binary-string input-value))))

(s/defn execute!
  []
  (let [syscall-code (a.number-base/bin->numeric (db.memory/read-value-by-name! "$v0"))]
    (case syscall-code
      1 (print-int!)
      4 (print-string!)
      5 (read-integer!)
      11 (print-char!)
      10 (exit!))))
