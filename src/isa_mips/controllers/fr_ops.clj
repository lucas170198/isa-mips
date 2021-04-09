(ns isa-mips.controllers.fr-ops
  (:require [schema.core :as s]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.memory :as db.memory]
            [clojure.string :as string]
            [clojure.string :as str]))

(s/defn ^:private format-extension
  [fmt]
  (let [fmt-value (a.number-base/bin->numeric fmt)]
    (cond
      (= fmt-value 17) :d
      (>= fmt-value 16) :s
      :else (throw (ex-info "Invalid format" {:format fmt})))))

(s/defn ^:private floating-point-move!
  [destiny-reg :- s/Str
   reg :- s/Str
   _]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-num      (a.number-base/bin->numeric reg)
        reg-value    (db.coproc1/read-value! reg-num)
        hi-value     (db.coproc1/read-value! (+ reg-num 1))]
    (db.coproc1/write-value! destiny-addr reg-value)
    (db.coproc1/write-value! (+ destiny-addr 1) hi-value)))

(s/defn ^:private move-float-to-reg!
  [_ :- s/Str
   reg :- s/Str
   regular-reg :- s/Str]
  (let [destiny-addr (a.number-base/bin->numeric regular-reg)
        reg-value    (db.coproc1/read-value! (a.number-base/bin->numeric reg))]
    (db.memory/write-value! destiny-addr reg-value)))

(s/defn ^:private move-word-to-float!
  [_ :- s/Str
   reg :- s/Str
   regular-reg :- s/Str]
  (let [read-addr    (a.number-base/bin->numeric regular-reg)
        destiny-addr (a.number-base/bin->numeric reg)
        reg-value    (db.memory/read-reg-value! (a.number-base/bin->numeric read-addr))]
    (db.coproc1/write-value! destiny-addr reg-value)))

(s/def ^:private fr-table-by-func
  {"000110" {:str "mov" :action floating-point-move!}
   "100001" {:str "cvt.d" :action nil}
   "000000" {:str "add"  :action nil}
   "000011" {:str "div" :action nil}
   "100000" {:str "cvt.s" :action nil}
   "000010" {:str "mul" :action nil}})

(s/def ^:private fr-table-by-fmt
  {"00000" {:str "mfc1" :action move-float-to-reg! :regular-reg true}
   "00100" {:str "mtc1" :action move-word-to-float! :regular-reg true}})

(s/defn ^:private with-formatted-name
  [func :- s/Str
   fmt :- s/Str]
  (when-let [operation (get fr-table-by-func func)]
    (update operation :str #(->> (format-extension fmt) name (str % ".")))))

(s/defn ^:private operation
  [func :- s/Str
   fmt :- s/Str]
  (or (get fr-table-by-fmt fmt)
      (with-formatted-name func fmt)))

(s/defn operation-str! :- s/Str
  [func :- s/Str
   fmt :- s/Str
   fd :- s/Str
   fs :- s/Str
   ft :- s/Str]
  (let [operation    (operation func fmt)
        func-name    (:str operation)
        _assert      (assert (not (nil? func-name)) (str "Operation not found on fr-table\n func: " func " fmt :" fmt))
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


