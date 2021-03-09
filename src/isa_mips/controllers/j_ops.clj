(ns isa-mips.controllers.j-ops
  (:require [schema.core :as s]
            [isa-mips.db.memory :as db.memory]))

(s/defn ^:private jump!
  [addr :- s/Str]
  (let [complete-addr (str addr "00")]
    (db.memory/set-program-counter (Integer/parseInt complete-addr 2))
    (db.memory/dec-program-counter))) ;TODO: Rataria, o PG é sempre incrementado no while

(s/def j-table
  {"000010" {:str "j" :action jump!}})

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
