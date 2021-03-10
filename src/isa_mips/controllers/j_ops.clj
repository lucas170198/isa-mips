(ns isa-mips.controllers.j-ops
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.helpers :as helpers]))

(s/defn ^:private jump!
  [addr :- s/Str]
  (let [complete-addr  (str addr "00")
        target-address (- (Integer/parseInt complete-addr 2) 4)] ;TODO: Rataria, o PC é sempre incrementado no while
    (db.memory/set-program-counter target-address)))

(s/defn ^:private jump-and-link!
  [addr :- s/Str]
  (let [ra-addr               31
        complete-addr         (str addr "00")
        next-instruction-addr (+ @db.memory/pc 4)
        target-address        (- (Integer/parseInt complete-addr 2) 4)] ;TODO: Rataria, o PC é sempre incrementado no while
    (db.memory/write-value! ra-addr (helpers/binary-string next-instruction-addr 32))
    (db.memory/set-program-counter target-address)))

(s/def j-table
  {"000010" {:str "j" :action jump!}
   "000011" {:str "jal" :action jump-and-link!}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   addr :- s/Str]
  (let [func-name (get-in j-table [func :str])]
    (str func-name " 0x" (Integer/toHexString (Integer/parseInt (str addr "00") 2)))))

(s/defn execute!
  [op-code :- s/Str
   addr :- s/Str]
  (let [func-fn (get-in j-table [(subs op-code 0 6) :action])]
    (func-fn addr)))
