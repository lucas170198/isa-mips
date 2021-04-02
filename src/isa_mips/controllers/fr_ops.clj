(ns isa-mips.controllers.fr-ops
  (:require [schema.core :as s]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.memory :as db.memory]
            [clojure.string :as string]))


(s/def fr-table
  {"000000" {:str "mfc1" :action nil :regular-reg true}
   "000110" {:str "mov.d" :action nil}})

(s/defn operation-str! :- s/Str
  [func :- s/Str
   fd :- s/Str
   fs :- s/Str
   ft :- s/Str]
  (let [operation    (get fr-table func)
        func-name    (:str operation)
        regular-reg? (:regular-reg operation)
        fd-name      (when-not regular-reg?
                       (db.coproc1/read-name! (a.number-base/bin->numeric fd)))
        fs-name      (db.coproc1/read-name! (a.number-base/bin->numeric fs))
        rt-name      (when regular-reg?
                       (db.memory/read-name! (a.number-base/bin->numeric ft)))]
    (str func-name " " (string/join ", " (remove nil? [rt-name fd-name fs-name])))))


