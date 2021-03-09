(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(def exit "code for exit application" -1)

(defn print-int!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      (Integer/parseInt 2)
      print))

(s/defn ^:private printable-array! []
  (loop [addr  (Integer/parseInt (db.memory/read-value-by-name! "$a0") 2)
         array '()]
    (let [character (Integer/parseInt (db.memory/read-value! addr) 2)]
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
      (Integer/parseInt 2)
      char
      print))

(defn read-integer!
  []
  (let [input-value (Integer/parseInt (read-line))]
    (db.memory/write-value! 2 (helpers/binary-string input-value))))

(s/defn execute!
  []
  (let [syscall-code (Integer/parseInt (db.memory/read-value-by-name! "$v0") 2)]
    (case syscall-code
      1 (print-int!)
      4 (print-string!)
      5 (read-integer!)
      11 (print-char!)
      10 exit)))
