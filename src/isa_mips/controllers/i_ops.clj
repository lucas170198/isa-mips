(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]
            [isa-mips.logic.binary :as l.binary]))

(s/defn ^:private addi!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg (Integer/parseInt destiny-reg 2)
        reg-bin     (db.memory/read-value! (Integer/parseInt reg 2))
        result      (l.binary/signed-sum reg-bin immediate)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result))))

(s/defn ^:private addiu!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg (Integer/parseInt destiny-reg 2)
        reg-bin     (db.memory/read-value! (Integer/parseInt reg 2))
        result      (l.binary/unsigned-sum reg-bin immediate)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result))))

(s/defn ^:private ori!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        immediate-value (Integer/parseInt immediate 2)
        reg-bin         (db.memory/read-value! (Integer/parseInt reg 2))
        result          (bit-or immediate-value (Integer/parseInt reg-bin 2))]
    (db.memory/write-value! destiny-reg (helpers/binary-string result 32))))

(s/defn ^:private andi
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        immediate-value (Integer/parseInt immediate 2)
        reg-bin         (db.memory/read-value! (Integer/parseInt reg 2))
        result          (bit-and immediate-value (Integer/parseInt reg-bin 2))]
    (db.memory/write-value! destiny-reg (helpers/binary-string result 32))))

(s/defn ^:private lui!
  [destiny-reg :- s/Str
   _reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        immediate-value (Integer/parseUnsignedInt immediate 2)
        result          (bit-shift-left immediate-value 16)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result 32))))

(s/defn ^:private branch-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [rt-bin          (db.memory/read-value! (Integer/parseInt destiny-reg 2))
        rs-bin          (db.memory/read-value! (Integer/parseInt reg 2))
        immediate-value (l.binary/bin->complement-of-two-int immediate)]
    (when (= (Integer/parseInt rt-bin 2) (Integer/parseInt rs-bin 2))
      (db.memory/sum-program-counter (* immediate-value 4)))))

(s/defn ^:private branch-not-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [rt-bin          (db.memory/read-value! (Integer/parseInt destiny-reg 2))
        rs-bin          (db.memory/read-value! (Integer/parseInt reg 2))
        immediate-value (l.binary/bin->complement-of-two-int immediate)]
    (when-not (= (Integer/parseInt rt-bin 2) (Integer/parseInt rs-bin 2))
      (db.memory/sum-program-counter (* immediate-value 4)))))

(s/def i-table
  {"001000" {:str "addi" :action addi!}
   "001001" {:str "addiu" :action addiu! :unsigned true}
   "001101" {:str "ori" :action ori!}
   "001100" {:str "andi" :action andi}
   "001111" {:str "lui" :action lui! :load-inst true}
   "000100" {:str "beq" :action branch-equal!}
   "000101" {:str "bne" :action branch-not-equal!}})

(s/defn operation-str! :- s/Str
  [op-code :- s/Str
   destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [operation        (get i-table (subs op-code 0 6))
        func-name        (:str operation)
        destiny-reg-name (db.memory/read-name! (Integer/parseInt destiny-reg 2))
        reg-name         (when-not (:load-inst operation) (db.memory/read-name! (Integer/parseInt reg 2)))
        unsigned?        (:unsigned operation)
        immediate-dec    (if unsigned? (Integer/parseUnsignedInt immediate 2) (l.binary/bin->complement-of-two-int immediate))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name reg-name immediate-dec])))))

(s/defn execute!
  [op-code destiny-reg reg immediate]
  (let [func-fn (get-in i-table [(subs op-code 0 6) :action])]
    (func-fn destiny-reg reg immediate)))
