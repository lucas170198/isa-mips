(ns isa-mips.controllers.r-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.memory :as db.memory]))

(s/defn add!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str])

;TODO: Table model schema
(s/def r-table
  {"100000" {:str "add" :action add!}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   destiny-reg :- s/Str
   first-reg :- s/Str
   second-reg :- s/Str]
  (let [func-name     (get-in r-table [func :str])
        destiny-reg-name (db.memory/read-name! (Integer/parseInt destiny-reg 2))
        first-reg-name   (db.memory/read-name! (Integer/parseInt first-reg 2))
        second-name      (db.memory/read-name! (Integer/parseInt second-reg 2))]
    (str func-name " " (string/join ", " [destiny-reg-name first-reg-name second-name]))))


