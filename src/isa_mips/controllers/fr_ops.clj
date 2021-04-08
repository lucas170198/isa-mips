(ns isa-mips.controllers.fr-ops
  (:require [schema.core :as s]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.memory :as db.memory]
            [clojure.string :as string]))



(s/defn ^:private floating-point-move!
  [destiny-reg :- s/Str
   reg :- s/Str
   _]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-num   (a.number-base/bin->numeric reg)
        reg-value (db.coproc1/read-value! reg-num)
        hi-value  (db.coproc1/read-value! (+ reg-num 1))]
    (db.coproc1/write-value! destiny-addr reg-value)
    (db.coproc1/write-value! (+ destiny-addr 1) hi-value)))

(s/defn ^:private move-float-to-reg!
  [_ :- s/Str
   reg :- s/Str
   regular-reg]
  (let [destiny-addr (a.number-base/bin->numeric regular-reg)
        reg-value (db.coproc1/read-value! (a.number-base/bin->numeric reg))]
    (db.memory/write-value! destiny-addr reg-value)))

(s/def fr-table-by-func
  {"000110" {:str "mov.d" :action floating-point-move!}})

(s/def fr-table-by-fmt
  {"00000" {:str "mfc1" :action move-float-to-reg! :regular-reg true}
   "00100" {:str "mtc1" :action nil}})


(s/defn ^:private operation
  [func :- s/Str
   fmt :- s/Str]
  (or (get fr-table-by-fmt fmt)
      (get fr-table-by-func func)))

(s/defn operation-str! :- s/Str
  [func :- s/Str
   fmt :- s/Str
   fd :- s/Str
   fs :- s/Str
   ft :- s/Str]
  (let [operation    (operation func fmt)
        func-name    (:str operation)
        regular-reg? (:regular-reg operation)
        fd-name      (when-not regular-reg?
                       (db.coproc1/read-name! (a.number-base/bin->numeric fd)))
        fs-name      (db.coproc1/read-name! (a.number-base/bin->numeric fs))
        rt-name      (when regular-reg?
                       (db.memory/read-name! (a.number-base/bin->numeric ft)))]
    (str func-name " " (string/join ", " (remove nil? [rt-name fd-name fs-name])))))

(s/defn execute!
  [func fmt fd fs ft]
  (let [func-fn (:action (operation func fmt))]
    (func-fn fd fs ft)))


