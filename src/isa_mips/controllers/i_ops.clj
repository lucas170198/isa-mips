(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(s/defn ^:private addi!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg (Integer/parseInt destiny-reg 2)
        reg-bin     (db.memory/read-value! (Integer/parseInt reg 2))
        result      (helpers/signed-sum reg-bin immediate)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result))))

(s/defn ^:private addiu!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg (Integer/parseInt destiny-reg 2)
        reg-bin     (db.memory/read-value! (Integer/parseInt reg 2))
        result      (helpers/unsigned-sum reg-bin immediate)]
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
        immediate-value (Integer/parseInt immediate 2)]
    (when (= rt-bin rs-bin) (db.memory/sum-program-counter (* immediate-value 4)))))

(s/defn ^:private branch-not-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [rt-bin          (db.memory/read-value! (Integer/parseInt destiny-reg 2))
        rs-bin          (db.memory/read-value! (Integer/parseInt reg 2))
        immediate-value (Integer/parseInt immediate 2)]
    (when-not (= rt-bin rs-bin) (db.memory/sum-program-counter (* immediate-value 4)))))

;TODO: Table model schema
(s/def i-table
  {"001000" {:str "addi" :action addi!}
   "001001" {:str "addiu" :action addiu! :unsigned true}
   "001101" {:str "ori" :action ori!}
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
        immediate-dec    (if unsigned? (Integer/parseUnsignedInt immediate 2) (Integer/parseInt immediate 2))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name reg-name immediate-dec])))))

(s/defn execute!
  [op-code destiny-reg reg immediate]
  (let [func-fn (get-in i-table [(subs op-code 0 6) :action])]
    (func-fn destiny-reg reg immediate)))
