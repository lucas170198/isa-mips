(ns isa-mips.controllers.r-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.helpers :as helpers]
            [isa-mips.controllers.text-section :as c.text-section]
            [isa-mips.adapters.number-base :as a.number-base]))

(s/defn ^:private add!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.memory/read-value! (a.number-base/bin->numeric rs))
        rt-bin  (db.memory/read-value! (a.number-base/bin->numeric rt))
        result  (l.binary/sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (helpers/binary-string result 32))))

(s/defn ^:private addu!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.memory/read-value! (a.number-base/bin->numeric rs))
        rt-bin  (db.memory/read-value! (a.number-base/bin->numeric rt))
        result  (l.binary/sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (helpers/binary-string result 32))))

(s/defn ^:private set-less-than!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr  (a.number-base/bin->numeric rd)
        rs-value (c.text-section/integer-reg-value! rs)
        rt-value (c.text-section/integer-reg-value! rt)
        result   (if (< rs-value rt-value) 1 0)]
    (db.memory/write-value! rd-addr (helpers/binary-string result 32))))

(s/defn ^:private jump-register!
  [_rd :- s/Str _rs :- s/Str _rt :- s/Str _shamt :- s/Str]
  (let [ra             (db.memory/read-value-by-name! "$ra")
        target-address (- (a.number-base/bin->numeric ra) 4)]       ;TODO: Rataria, PC
    (db.memory/set-program-counter target-address)))

(s/defn ^:private shift-left!
  [rd :- s/Str _rs :- s/Str rt :- s/Str shamt :- s/Str]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-value    (-> (a.number-base/bin->numeric rt) (db.memory/read-value!) a.number-base/bin->numeric)
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-left rt-value shamt-value)]
    (db.memory/write-value! rd-addr (helpers/binary-string result 32))))

(s/defn ^:private shift-right!
  [rd :- s/Str _rs :- s/Str rt :- s/Str shamt :- s/Str]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-value    (c.text-section/integer-reg-value! rt)
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-right rt-value shamt-value)]
    (db.memory/write-value! rd-addr (helpers/binary-string result 32))))

;TODO: Table model schema
(s/def r-table
  {"100000" {:str "add" :action add!}
   "100001" {:str "addu" :action addu! :unsigned true}
   "101010" {:str "slt" :action set-less-than!}
   "001000" {:str "jr" :action jump-register! :jump-instruction true}
   "000000" {:str "sll" :action shift-left! :shamt true}
   "000010" {:str "srl" :action shift-right! :shamt true}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str
   shamt :- s/Str]
  (let [operation         (get r-table func)
        func-name         (:str operation)
        jump-instruction? (:jump-instruction operation)
        shamt?            (:shamt operation)
        destiny-reg-name  (when-not jump-instruction? (db.memory/read-name! (a.number-base/bin->numeric destiny-reg)))
        first-reg-name    (when-not shamt? (db.memory/read-name! (a.number-base/bin->numeric first-reg)))
        shamt             (when shamt? (a.number-base/bin->numeric shamt))
        second-name       (when-not jump-instruction?
                            (db.memory/read-name! (a.number-base/bin->numeric second-reg)))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name first-reg-name second-name shamt])))))

(s/defn execute!
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str
   shamt :- s/Str]
  (let [func-fn (get-in r-table [func :action])]
    (func-fn destiny-reg first-reg second-reg shamt)))


