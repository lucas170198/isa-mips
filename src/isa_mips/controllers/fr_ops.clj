(ns isa-mips.controllers.fr-ops
  (:require [schema.core :as s]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.registers :as db.registers]
            [clojure.string :as string]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.models.instruction :as m.instruction]))

(s/defn ^:private format-extension
  [fmt]
  (let [fmt-value (a.number-base/bin->numeric fmt)]
    (cond
      (= fmt-value 17) :d
      (>= fmt-value 16) :w
      :else (throw (ex-info "Invalid format" {:format fmt})))))

(s/defn ^:private move-float-to-reg!
  [_ :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   _
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric regular-reg)
        reg-value    (db.coproc1/read-value! (a.number-base/bin->numeric reg) coproc-storage)]
    (db.registers/write-value! destiny-addr reg-value storage)))

(s/defn ^:private move-word-to-float!
  [_ :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   _
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [read-addr    (a.number-base/bin->numeric regular-reg)
        destiny-addr (a.number-base/bin->numeric reg)
        reg-value    (db.registers/read-reg-value! read-addr storage)]
    (db.coproc1/write-value! destiny-addr reg-value coproc-storage)))

(s/defn ^:private double-move!
  [destiny-reg :- s/Str
   reg :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr     (a.number-base/bin->numeric destiny-reg)
        double-bin-value (db.coproc1/load-double-from-memory! reg coproc-storage)]
    (db.coproc1/write-double-on-memory! destiny-addr double-bin-value coproc-storage)))

(s/defn ^:private floating-point-move!
  [destiny-reg :- s/Str
   reg :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-num      (a.number-base/bin->numeric reg)
        reg-value    (db.coproc1/read-value! reg-num coproc-storage)]
    (db.coproc1/write-value! destiny-addr reg-value coproc-storage)))

(s/defn ^:private mov!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   copco-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (floating-point-move! destiny-reg reg regular-reg copco-storage)
    :d (double-move! destiny-reg regular-reg regular-reg copco-storage)))

(s/defn ^:private convert-integer-to-double!
  [destiny-reg :- s/Str
   reg :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/read-value! (a.number-base/bin->numeric reg) coproc-storage)
        result       (double (a.number-base/bin->numeric reg-bin))
        result-bin   (l.binary/zero-extend-nbits (a.number-base/double->bin result) 64)]
    (db.coproc1/write-double-on-memory! destiny-addr result-bin coproc-storage)))

(s/defn ^:private cvt-d!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (convert-integer-to-double! destiny-reg reg regular-reg coproc-storage)
    :d (throw (ex-info "Not implemented format" {:func "cvt.d"}))))

(s/defn ^:private float-add!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/read-value! (a.number-base/bin->numeric reg) coproc-storage)
        reg2-bin     (db.coproc1/read-value! (a.number-base/bin->numeric reg2) coproc-storage)
        result       (+ (a.number-base/bin->float reg-bin) (a.number-base/bin->float reg2-bin))]
    (db.coproc1/write-value! destiny-addr (l.binary/zero-extend-32bits (a.number-base/float->bin result)) coproc-storage)))

(s/defn ^:private double-add!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/load-double-from-memory! reg coproc-storage)
        reg2-bin     (db.coproc1/load-double-from-memory! reg2 coproc-storage)
        result       (+ (a.number-base/bin->double reg-bin) (a.number-base/bin->double reg2-bin))
        result-bin   (l.binary/zero-extend-nbits (a.number-base/double->bin result) 64)]
    (db.coproc1/write-double-on-memory! destiny-addr result-bin coproc-storage)))

(s/defn ^:private add!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (float-add! destiny-reg reg regular-reg coproc-storage)
    :d (double-add! destiny-reg reg regular-reg coproc-storage)))

(s/defn ^:private double-div!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/load-double-from-memory! reg coproc-storage)
        reg2-bin     (db.coproc1/load-double-from-memory! reg2 coproc-storage)
        result       (/ (a.number-base/bin->double reg-bin) (a.number-base/bin->double reg2-bin))
        result-bin   (l.binary/zero-extend-nbits (a.number-base/double->bin result) 64)]
    (db.coproc1/write-double-on-memory! destiny-addr result-bin coproc-storage)))

(s/defn ^:private div!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (throw (ex-info "Not implemented format" {:func "div"}))
    :d (double-div! destiny-reg reg regular-reg coproc-storage)))

(s/defn ^:private double-to-single!
  [destiny-reg :- s/Str
   reg :- s/Str
   _ :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        value-bin    (db.coproc1/load-double-from-memory! reg coproc-storage)
        value        (a.number-base/bin->double value-bin)
        result       (a.number-base/float->bin (float value))]
    (db.coproc1/write-value! destiny-addr (l.binary/zero-extend-32bits result) coproc-storage)))

(s/defn ^:private cvt-s!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (throw (ex-info "Not implemented format" {:func "cvt-s"}))
    :d (double-to-single! destiny-reg reg regular-reg coproc-storage)))

(s/defn ^:private multiply-single!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-attr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/read-value! (a.number-base/bin->numeric reg) coproc-storage)
        reg2-bin     (db.coproc1/read-value! (a.number-base/bin->numeric reg2) coproc-storage)
        result       (* (a.number-base/bin->float reg-bin) (a.number-base/bin->float reg2-bin))]
    (db.coproc1/write-value! destiny-attr (l.binary/zero-extend-32bits (a.number-base/float->bin result)) coproc-storage)))

(s/defn ^:private multiply-double!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/load-double-from-memory! reg coproc-storage)
        reg2-bin     (db.coproc1/load-double-from-memory! reg2 coproc-storage)
        result       (* (a.number-base/bin->double reg-bin) (a.number-base/bin->double reg2-bin))
        result-bin   (l.binary/zero-extend-nbits (a.number-base/double->bin result) 64)]
    (db.coproc1/write-double-on-memory! destiny-addr result-bin coproc-storage)))

(s/defn ^:private mul!
  [destiny-reg :- s/Str
   reg :- s/Str
   regular-reg :- s/Str
   fmt :- s/Str
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (multiply-single! destiny-reg reg regular-reg coproc-storage)
    :d (multiply-double! destiny-reg reg regular-reg coproc-storage)))

(s/def ^:private fr-table-by-func
  {"000110" {:str "mov" :action mov!}
   "100001" {:str "cvt.d" :action cvt-d!}
   "000000" {:str "add" :action add! :second-reg true}
   "000011" {:str "div" :action div! :second-reg true}
   "100000" {:str "cvt.s" :action cvt-s!}
   "000010" {:str "mul" :action mul! :second-reg true}})

(s/def ^:private fr-table-by-fmt
  {"00000" {:str "mfc1" :action move-float-to-reg! :second-reg true :mem-reg true}
   "00100" {:str "mtc1" :action move-word-to-float! :second-reg true :mem-reg true}})

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
  [{:keys [funct fmt fd fs ft]} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [operation   (operation funct fmt)
        func-name   (:str operation)
        _assert     (assert (not (nil? func-name)) (str "Operation not found on fr-table\n func: " funct " fmt :" fmt))
        second-reg? (:second-reg operation)
        mem-reg?    (:mem-reg operation)
        fd-name     (when-not mem-reg? (db.coproc1/read-name! (a.number-base/bin->numeric fd) coproc-storage))
        fs-name     (db.coproc1/read-name! (a.number-base/bin->numeric fs) coproc-storage)
        rt-name     (when second-reg? (db.registers/read-name! (a.number-base/bin->numeric ft) storage))]
    (str func-name " " (string/join ", " (remove nil? [rt-name fd-name fs-name])))))

(s/defn execute!
  [{:keys [funct fmt fd fs ft]} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [func-fn (:action (operation funct fmt))]
    (func-fn fd fs ft fmt storage coproc-storage)))
