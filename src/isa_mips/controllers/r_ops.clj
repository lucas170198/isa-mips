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
  (let [ra (db.memory/read-value-by-name! "$ra")
        target-address (- (Integer/parseInt ra 2) 4)] ;TODO: Rataria, PC
    (db.memory/set-program-counter target-address)))

;TODO: Table model schema
(s/def r-table
  {"100000" {:str "add" :action add! :signed true}
   "100001" {:str "addu" :action addu! :signed false}
   "101010" {:str "slt" :action set-less-than! :signed true}
   "001000" {:str "jr" :action jump-register! :signed true :jump-instruction true}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str]
  (let [operation         (get r-table func)
        func-name         (:str operation)
        jump-instruction? (:jump-instruction operation)
        destiny-reg-name  (when-not jump-instruction? (db.memory/read-name! (Integer/parseInt destiny-reg 2)))
        first-reg-name    (db.memory/read-name! (Integer/parseInt first-reg 2))
        second-name       (when-not jump-instruction? (db.memory/read-name! (Integer/parseInt second-reg 2)))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name first-reg-name second-name])))))

(s/defn execute!
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str]
  (let [func-fn (get-in r-table [func :action])]
    (func-fn destiny-reg first-reg second-reg)))


