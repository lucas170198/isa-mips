(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.logic.binary :as l.binary]))

(defn exit! []
  (println)
  (System/exit 0))

(defn print-int!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      a.number-base/bin->numeric
      print)
  (flush))

(defn print-float!
  []
  (-> (db.coproc1/read-value-by-name! "$f12")
      a.number-base/bin->float
      print)
  (flush))

(defn print-double!
  []
  (let [double-string (str (db.coproc1/read-value-by-name! "$f13")
                           (db.coproc1/read-value-by-name! "$f12"))]
    (print (a.number-base/bin->double double-string)))
  (flush))

(s/defn ^:private printable-array! []
  (loop [addr  (a.number-base/bin->numeric (db.memory/read-value-by-name! "$a0"))
         array '()]
    (let [character (a.number-base/bin->numeric (db.memory/read-reg-value! addr))]
      (if (= character 0)
        array
        (recur (inc addr)
               (concat array (list (char character))))))))

(defn print-string!
  []
  (->> (printable-array!)
       (apply str)
       print)
  (flush))

(defn print-char!
  []
  (-> (db.memory/read-value-by-name! "$a0")
      a.number-base/bin->numeric
      char
      print)
  (flush))

(defn read-integer!
  []
  (let [input-value (Integer/parseInt (read-line))]
    (db.memory/write-value! 2 (a.number-base/binary-string-zero-extend input-value 32))))

(defn read-float!
  []
  (let [input-value (Float/parseFloat (read-line))]
    (db.coproc1/write-value! 0 (a.number-base/float->bin input-value))))

(defn read-double!
  []
  (let [input-value (Double/parseDouble (read-line))
        bin (l.binary/zero-extend-nbits (a.number-base/double->bin input-value) 64)]
    (db.coproc1/write-value! 1 (subs bin 0 32))
    (db.coproc1/write-value! 0 (subs bin 32 64))))


(s/defn execute!
  []
  (let [syscall-code (a.number-base/bin->numeric (db.memory/read-value-by-name! "$v0"))]
    (case syscall-code
      1 (print-int!)
      2 (print-float!)
      3 (print-double!)
      4 (print-string!)
      5 (read-integer!)
      6 (read-float!)
      7 (read-double!)
      11 (print-char!)
      10 (exit!))))
