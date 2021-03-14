(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.adapters.number-base :as a.number-base]))

(s/defn ^:private addi!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg      (a.number-base/bin->numeric destiny-reg)
        reg-bin          (db.memory/read-value! (a.number-base/bin->numeric reg))
        immediate-signal (l.binary/signal-extend-32bits immediate)
        result           (l.binary/sum reg-bin immediate-signal)]
    (db.memory/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private addiu!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg      (a.number-base/bin->numeric destiny-reg)
        reg-bin          (db.memory/read-value! (a.number-base/bin->numeric reg))
        immediate-signal (l.binary/signal-extend-32bits immediate)
        result           (l.binary/sum reg-bin immediate-signal)]
    (db.memory/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private ori!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (a.number-base/bin->numeric destiny-reg)
        immediate-value (a.number-base/bin->numeric (l.binary/zero-extend-32bits immediate))
        reg-bin         (db.memory/read-value! (a.number-base/bin->numeric reg))
        result          (bit-or immediate-value (a.number-base/bin->numeric reg-bin))]
    (db.memory/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private andi
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (a.number-base/bin->numeric destiny-reg)
        immediate-value (a.number-base/bin->numeric (l.binary/zero-extend-32bits immediate))
        reg-bin         (db.memory/read-value! (a.number-base/bin->numeric reg))
        result          (bit-and immediate-value (a.number-base/bin->numeric reg-bin))]
    (db.memory/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32))))

(s/defn ^:private lui!
  [destiny-reg :- s/Str
   _reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (a.number-base/bin->numeric destiny-reg)
        result          (str immediate (apply str (repeat 16 "0")))]
    (db.memory/write-value! destiny-reg result)))

;TODO
(s/defn ^:private branch-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [rt-bin          (db.memory/read-value! (a.number-base/bin->numeric destiny-reg))
        rs-bin          (db.memory/read-value! (a.number-base/bin->numeric reg))
        immediate-value (a.number-base/bin->numeric (l.binary/signal-extend-32bits immediate))]
    (when (= (a.number-base/bin->numeric rt-bin) (a.number-base/bin->numeric rs-bin))
      (db.memory/sum-program-counter (* immediate-value 4)))))
;TODO
(s/defn ^:private branch-not-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [rt-bin          (db.memory/read-value! (a.number-base/bin->numeric destiny-reg))
        rs-bin          (db.memory/read-value! (a.number-base/bin->numeric reg))
        immediate-value (a.number-base/bin->numeric (l.binary/signal-extend-32bits immediate))]
    (when-not (= (a.number-base/bin->numeric rt-bin) (a.number-base/bin->numeric rs-bin))
      (db.memory/sum-program-counter (* immediate-value 4)))))

(s/defn ^:private load-word!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        offset       (l.binary/signal-extend-32bits immediate)
        reg-bin      (db.memory/read-value! (a.number-base/bin->numeric reg))
        target-addr  (l.binary/sum reg-bin offset)
        memory-value (db.memory/read-value! target-addr)]
    (db.memory/write-value! destiny-addr memory-value)))

(s/defn ^:private store-word!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-value (db.memory/read-value! (a.number-base/bin->numeric destiny-reg))
        offset        (l.binary/signal-extend-32bits immediate)
        reg-bin       (db.memory/read-value! (a.number-base/bin->numeric reg))
        target-addr   (l.binary/sum reg-bin offset)]
    (db.memory/write-value! target-addr destiny-value)))

(s/def i-table
  {"001000" {:str "addi" :action addi!}
   "001001" {:str "addiu" :action addiu! :unsigned true}
   "001101" {:str "ori" :action ori!}
   "001100" {:str "andi" :action andi}
   "001111" {:str "lui" :action lui! :load-inst true}
   "000100" {:str "beq" :action branch-equal!}
   "000101" {:str "bne" :action branch-not-equal!}
   "100011" {:str "lw" :action load-word! :memory-op true}
   "101011" {:str "sw" :action store-word! :memory-op true}})

(s/defn operation-str! :- s/Str
  [op-code :- s/Str
   destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [operation        (get i-table (subs op-code 0 6))
        func-name        (:str operation)
        _assert          (assert (not (nil? func-name)) "Operation not found on i-table")
        destiny-reg-name (db.memory/read-name! (a.number-base/bin->numeric destiny-reg))
        reg-name         (when-not (:load-inst operation) (db.memory/read-name! (a.number-base/bin->numeric reg)))
        unsigned?        (:unsigned operation)
        memory-op?       (:memory-op operation)
        immediate-dec    (if unsigned? (Integer/parseUnsignedInt immediate 2) (l.binary/bin->complement-of-two-int immediate))]
    (if memory-op?
      (str func-name " " destiny-reg ", " (l.binary/bin->hex-str immediate) "(" reg-name ")")
      (str func-name " " (string/join ", " (remove nil? [destiny-reg-name reg-name immediate-dec]))))))

(s/defn execute!
  [op-code destiny-reg reg immediate]
  (let [func-fn (get-in i-table [(subs op-code 0 6) :action])]
    (func-fn destiny-reg reg immediate)))
