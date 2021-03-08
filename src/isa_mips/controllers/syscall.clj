(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]))

(def exit "code for exit application" -1)

(defn print-int!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      (Integer/parseInt  2)
      println))

(s/defn ^:private printable-array! []
  (loop [addr  (Integer/parseInt (db.memory/read-value-by-name! "$a0") 2)
         array '()]
    (let [character (Integer/parseInt (db.memory/read-value! addr) 2)]
      (if (= character 0)
        array
        (recur (inc addr)
               (concat array (list (byte character))))))))

(defn print-string!
  []
  (-> (printable-array!)
      byte-array
      String.
      println))

(defn print-char!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      (Integer/parseInt 2)
      char
      println))

(s/defn execute!
  []
  (let [syscall-code (Integer/parseInt (db.memory/read-value-by-name! "$v0") 2)]
    (case syscall-code
      1 (print-int!)
      4 (print-string!)
      11 (print-char!)
      10 exit)))
