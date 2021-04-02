(ns isa-mips.controllers.r-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.adapters.number-base :as a.number-base]))

(s/defn ^:private add!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.memory/read-value! (a.number-base/bin->numeric rs))
        rt-bin  (db.memory/read-value! (a.number-base/bin->numeric rt))
        result  (l.binary/signed-sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private addu!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.memory/read-value! (a.number-base/bin->numeric rs))
        rt-bin  (db.memory/read-value! (a.number-base/bin->numeric rt))
        result  (l.binary/sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private set-less-than!
  [rd :- s/Str rs :- s/Str rt :- s/Str _shamt :- s/Str]
  (let [rd-addr  (a.number-base/bin->numeric rd)
        rs-bin   (db.memory/read-value! (a.number-base/bin->numeric rs))
        rt-bin   (db.memory/read-value! (a.number-base/bin->numeric rt))
        result   (if (< (a.number-base/bin->numeric rs-bin)
                        (a.number-base/bin->numeric rt-bin)) 1 0)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-zero-extend result 32))))

(s/defn ^:private jump-register!
  [_rd :- s/Str rs :- s/Str _rt :- s/Str _shamt :- s/Str]
  (let [target-address             (db.memory/read-value! (a.number-base/bin->numeric rs))]
    (db.memory/set-jump-addr! (a.number-base/bin->numeric target-address))))

(s/defn ^:private shift-left!
  [rd :- s/Str _rs :- s/Str rt :- s/Str shamt :- s/Str]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-bin      (db.memory/read-value! (a.number-base/bin->numeric rt))
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-left (a.number-base/bin->numeric rt-bin) shamt-value)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private shift-right!
  [rd :- s/Str _rs :- s/Str rt :- s/Str shamt :- s/Str]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-bin      (db.memory/read-value! (a.number-base/bin->numeric rt))
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-right (a.number-base/bin->numeric rt-bin) shamt-value)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32))))

(s/defn r-jump-and-link!
  [rd :- s/Str rs :- s/Str _rt :- s/Str _shamt :- s/Str]
  (let [rd-addr               (a.number-base/bin->numeric rd)
        jump-addr             (db.memory/read-value! (a.number-base/bin->numeric rs))
        next-instruction-addr (+ @db.memory/pc 4)]
    (db.memory/write-value! rd-addr (a.number-base/binary-string-zero-extend next-instruction-addr 32))
    (db.memory/set-jump-addr! (- (Integer/parseUnsignedInt jump-addr 2) 4))))

(s/def r-table
  {"100000" {:str "add" :action add!}
   "100001" {:str "addu" :action addu! :unsigned true}
   "101010" {:str "slt" :action set-less-than!}
   "001000" {:str "jr" :action jump-register! :hide-destiny-reg true :hide-second-reg true}
   "000000" {:str "sll" :action shift-left! :shamt true}
   "000010" {:str "srl" :action shift-right! :shamt true}
   "001001" {:str "jalr" :action r-jump-and-link! :hide-second-reg true}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str
   shamt :- s/Str]
  (let [operation         (get r-table func)
        func-name         (:str operation)
        _assert           (assert (not (nil? operation)) (str "Operation not found on r-table\n func: " func))
        shamt?            (:shamt operation)
        destiny-reg-name  (when-not (:hide-destiny-reg operation) (db.memory/read-name! (a.number-base/bin->numeric destiny-reg)))
        first-reg-name    (when-not shamt? (db.memory/read-name! (a.number-base/bin->numeric first-reg)))
        shamt             (when shamt? (a.number-base/bin->numeric shamt))
        second-name       (when-not (:hide-second-reg operation)
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


