(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]))

(def exit "code for exit application" -1)

(defn print-int!
  []
  (println (Integer/parseInt (db.memory/read-value-by-name! "$a0") 2)))

(s/defn execute!
  []
  (let [syscall-code (Integer/parseInt (db.memory/read-value-by-name! "$v0") 2)]
    (case syscall-code
      1 (print-int!)
      10 exit)))
