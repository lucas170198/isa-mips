(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(s/defn addi!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        reg-bin         (db.memory/read-value! (Integer/parseInt reg 2))
        result          (helpers/signed-sum reg-bin immediate)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result))))

(s/defn addiu!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        reg-bin         (db.memory/read-value! (Integer/parseInt reg 2))
        result          (helpers/unsigned-sum reg-bin immediate)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result))))

(s/defn ori!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        immediate-value (Integer/parseInt immediate 2)
        reg-bin         (db.memory/read-value! (Integer/parseInt reg 2))
        result          (bit-or immediate-value (Integer/parseInt reg-bin 2))]
    (db.memory/write-value! destiny-reg (helpers/binary-string result 32))))

(s/defn lui!
  [destiny-reg :- s/Str
   _reg :- s/Str
   immediate :- s/Str]
  (let [destiny-reg     (Integer/parseInt destiny-reg 2)
        immediate-value (Integer/parseUnsignedInt immediate 2)
        result          (bit-shift-left immediate-value 16)]
    (db.memory/write-value! destiny-reg (helpers/binary-string result 32))))

(s/defn beq!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  "FIX ME")

(s/defn bne!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  "FIX ME")

;TODO: Table model schema
(s/def i-table
  {"001000" {:str "addi" :action addi! :signed true :load-inst false}
   "001001" {:str "addiu" :action addiu! :signed false :load-inst false}
   "001101" {:str "ori" :action ori! :signed true :load-inst false}
   "001111" {:str "lui" :action lui! :signed true :load-inst true}
   "000100" {:str "beq" :action beq! :signed true :load-inst false}
   "000101" {:str "bne" :action bne! :signed true :load-inst false}})

(s/defn operation-str! :- s/Str
  [op-code :- s/Str
   destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [operation        (get i-table (subs op-code 0 6))
        func-name        (:str operation)
        destiny-reg-name (db.memory/read-name! (Integer/parseInt destiny-reg 2))
        reg-name         (when-not (:load-inst operation) (db.memory/read-name! (Integer/parseInt reg 2)))
        signed?          (:signed operation)
        immediate-dec    (if signed? (Integer/parseInt immediate 2) (Integer/parseUnsignedInt immediate 2))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name reg-name immediate-dec])))))

(s/defn execute!
  [op-code destiny-reg reg immediate]
  (let [func-fn (get-in i-table [(subs op-code 0 6) :action])]
    (func-fn destiny-reg reg immediate)))
