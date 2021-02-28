(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]))

;TODO
(s/defn addi!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str])

;TODO: Table model schema
(s/def i-table
  {"000" {:str "addi" :action addi!}})

(s/defn operation-str! :- s/Str
  [op-code :- s/Str
   destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str]
  (let [func-name        (get-in i-table [(subs op-code 3 6) :str])
        destiny-reg-name (db.memory/read-name! (Integer/parseInt destiny-reg 2))
        reg-name         (db.memory/read-name! (Integer/parseInt reg 2))
        immediate-dec    (Integer/parseInt immediate 2)]
    (str func-name " " (string/join ", " [destiny-reg-name reg-name immediate-dec]))))
