(ns isa-mips.controllers.j-ops
  (:require [schema.core :as s]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.models.instruction :as m.instruction]))

(s/defn ^:private jump!
  [addr :- s/Str
   _]
  (let [next-inst     (a.number-base/binary-string-zero-extend (+ @db.registers/pc 4) 32)
        complete-addr (a.number-base/bin->numeric (str (subs next-inst 0 4) addr "00"))]
    (db.registers/set-jump-addr! (- complete-addr 4))))

(s/defn ^:private jump-and-link!
  [addr :- s/Str
   storage :- p-storage/IStorageClient]
  (let [ra-addr               31
        next-inst             (a.number-base/binary-string-zero-extend (+ @db.registers/pc 4) 32)
        jump-addr             (a.number-base/bin->numeric (str (subs next-inst 0 4) addr "00"))
        next-instruction-addr (+ @db.registers/pc 4)]
    (db.registers/write-value! ra-addr (a.number-base/binary-string-zero-extend next-instruction-addr 32) storage)
    (db.registers/set-jump-addr! (- jump-addr 4))))

(s/def j-table
  {"000010" {:str "j" :action jump!}
   "000011" {:str "jal" :action jump-and-link!}})

(s/defn operation-str! :- s/Str
  [{op-code :op
    addr    :target-address} :- m.instruction/JInstruction]
  (let [func-name (get-in j-table [op-code :str])
        _assert   (assert (not (nil? func-name)) "Operation not found on j-table")]
    (str func-name (l.binary/bin->hex-str (str addr "00")))))

(s/defn execute!
  [{op-code :op
    addr    :target-address} :- m.instruction/JInstruction
   storage :- p-storage/IStorageClient]
  (let [func-fn (get-in j-table [(subs op-code 0 6) :action])]
    (func-fn addr storage)))
