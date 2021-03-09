(ns isa-mips.controllers.j-ops
  (:require [schema.core :as s]))

(s/defn ^:private jump! [])

(s/def j-table
  {"000010" {:str "j" :action jump!}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   addr :- s/Str]
  (let [func-name (get-in j-table [func :str])]
    (str func-name " 0x" (Integer/toHexString (Integer/parseInt (str addr "00") 2)))))
