(ns isa-mips.controllers.syscall
  (:require [schema.core :as s]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.logic.binary :as l.binary]))

(defn exit! []
  (println)
  (System/exit 0))

(defn print-int!
  [storage]
  (-> (db.registers/read-value-by-name! "$a0" storage)
      a.number-base/bin->numeric
      print)
  (flush))

(defn print-float!
  [coproc-storage]
  (-> (db.coproc1/read-value-by-name! "$f12" coproc-storage)
      a.number-base/bin->float
      print)
  (flush))

(defn print-double!
  [coproc-storage]
  (let [double-string (str (db.coproc1/read-value-by-name! "$f13" coproc-storage)
                           (db.coproc1/read-value-by-name! "$f12" coproc-storage))]
    (print (a.number-base/bin->double double-string)))
  (flush))

(s/defn ^:private printable-array! [storage]
  (loop [addr  (a.number-base/bin->numeric (db.registers/read-value-by-name! "$a0" storage))
         array '()]
    (let [character (a.number-base/bin->numeric (db.registers/read-reg-value! addr storage))]
      (if (= character 0)
        array
        (recur (inc addr)
               (concat array (list (char character))))))))

(defn print-string!
  [storage]
  (->> (printable-array! storage)
       (apply str)
       print)
  (flush))

(defn print-char!
  [storage]
  (-> (db.registers/read-value-by-name! "$a0" storage)
      a.number-base/bin->numeric
      char
      print)
  (flush))

(defn read-integer!
  [storage]
  (let [input-value (Integer/parseInt (read-line))]
    (db.registers/write-value! 2 (a.number-base/binary-string-zero-extend input-value 32) storage)))

(defn read-float!
  [coproc-storage]
  (let [input-value (Float/parseFloat (read-line))]
    (db.coproc1/write-value! 0 (a.number-base/float->bin input-value) coproc-storage)))

(defn read-double!
  [coproc-storage]
  (let [input-value (Double/parseDouble (read-line))
        bin (l.binary/zero-extend-nbits (a.number-base/double->bin input-value) 64)]
    (db.coproc1/write-value! 1 (subs bin 0 32) coproc-storage)
    (db.coproc1/write-value! 0 (subs bin 32 64) coproc-storage)))


(s/defn execute!
  [storage coproc-storage]
  (let [syscall-code (a.number-base/bin->numeric (db.registers/read-value-by-name! "$v0" storage))]
    (case syscall-code
      1 (print-int! storage)
      2 (print-float! coproc-storage)
      3 (print-double! coproc-storage)
      4 (print-string! storage)
      5 (read-integer! storage)
      6 (read-float! coproc-storage)
      7 (read-double! coproc-storage)
      11 (print-char! storage)
      10 (exit!))))
