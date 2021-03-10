(ns isa-mips.controllers.r-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(s/defn ^:private add!
  [rd :- s/Str rs :- s/Str rt :- s/Str]
  (let [rd-addr (Integer/parseInt rd 2)
        rs-bin  (db.memory/read-value! (Integer/parseInt rs 2))
        rt-bin  (db.memory/read-value! (Integer/parseInt rt 2))
        result  (helpers/signed-sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (helpers/binary-string result))))

(s/defn ^:private addu!
  [rd :- s/Str rs :- s/Str rt :- s/Str]
  (let [rd-addr (Integer/parseInt rd 2)
        rs-bin  (db.memory/read-value! (Integer/parseInt rs 2))
        rt-bin  (db.memory/read-value! (Integer/parseInt rt 2))
        result  (helpers/unsigned-sum rs-bin rt-bin)]
    (db.memory/write-value! rd-addr (helpers/binary-string result))))

(s/defn ^:private set-less-than!
  [rd :- s/Str rs :- s/Str rt :- s/Str]
  (let [rd-addr  (Integer/parseInt rd 2)
        rs-value (-> (Integer/parseInt rs 2) (db.memory/read-value!) (Integer/parseInt 2))
        rt-value (-> (Integer/parseInt rt 2) (db.memory/read-value!) (Integer/parseInt 2))
        result   (if (< rs-value rt-value) 1 0)]
    (db.memory/write-value! rd-addr (helpers/binary-string result))))

(s/defn ^:private jump-register!
  [_rd :- s/Str _rs :- s/Str _rt :- s/Str]
  (let [ra             (db.memory/read-value-by-name! "$ra")
        target-address (- (Integer/parseInt ra 2) 4)]       ;TODO: Rataria, PC
    (db.memory/set-program-counter target-address)))

(s/defn ^:private shift-left!
  [_rd :- s/Str _rs :- s/Str _rt :- s/Str])

(s/defn ^:private shift-right!
  [_rd :- s/Str _rs :- s/Str _rt :- s/Str])

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
        destiny-reg-name  (when-not jump-instruction? (db.memory/read-name! (Integer/parseInt destiny-reg 2)))
        first-reg-name    (when-not shamt? (db.memory/read-name! (Integer/parseInt first-reg 2)))
        shamt             (when shamt? (str (Integer/parseInt shamt 2)))
        second-name       (when-not jump-instruction?
                            (db.memory/read-name! (Integer/parseInt second-reg 2)))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name first-reg-name second-name shamt])))))

(s/defn execute!
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str]
  (let [func-fn (get-in r-table [func :action])]
    (func-fn destiny-reg first-reg second-reg)))


