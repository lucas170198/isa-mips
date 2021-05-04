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
  [{:keys [fs ft]} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric ft)
        reg-value    (db.coproc1/read-value! (a.number-base/bin->numeric fs) coproc-storage)]
    (db.registers/write-value! destiny-addr reg-value storage)))

(s/defn ^:private move-word-to-float!
  [{:keys [fs ft]} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [read-addr    (a.number-base/bin->numeric ft)
        destiny-addr (a.number-base/bin->numeric fs)
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
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   copco-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (floating-point-move! fd fs ft copco-storage)
    :d (double-move! fd fs ft copco-storage)))

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
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (convert-integer-to-double! fd fs ft coproc-storage)
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

(s/defn ^:private float-sub!
  [destiny-reg :- s/Str
   reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        reg-bin      (db.coproc1/read-value! (a.number-base/bin->numeric reg) coproc-storage)
        reg2-bin     (db.coproc1/read-value! (a.number-base/bin->numeric reg2) coproc-storage)
        result       (- (a.number-base/bin->float reg-bin) (a.number-base/bin->float reg2-bin))]
    (db.coproc1/write-value! destiny-addr (l.binary/zero-extend-32bits (a.number-base/float->bin result)) coproc-storage)))

(s/defn ^:private add!
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (float-add! fd fs ft coproc-storage)
    :d (double-add! fd fs ft coproc-storage)))

(s/defn ^:private sub!
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (float-sub! fd fs ft coproc-storage)
    :d (throw (ex-info "Not implemented format" {:func "sub"}))))

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
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (throw (ex-info "Not implemented format" {:func "div"}))
    :d (double-div! fd fs ft coproc-storage)))

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
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (throw (ex-info "Not implemented format" {:func "cvt-s"}))
    :d (double-to-single! fd fs ft coproc-storage)))

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
  [{:keys [fmt fd fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (multiply-single! fd fs ft coproc-storage)
    :d (multiply-double! fd fs ft coproc-storage)))

(s/defn ^:private compare-less-then-float!
  [reg :- s/Str
   reg2 :- s/Str
   coproc-storage :- p-storage/IStorageClient]
  (let [reg-bin  (db.coproc1/load-double-from-memory! reg coproc-storage)
        reg2-bin (db.coproc1/load-double-from-memory! reg2 coproc-storage)]
    (if (< (a.number-base/bin->float reg-bin) (a.number-base/bin->float reg2-bin))
      (db.coproc1/set-condition-flag!)
      (db.coproc1/reset-condition-flag!))))

(s/defn ^:private c-lt!
  [{:keys [fmt fs ft]} :- m.instruction/FRInstruction
   _
   coproc-storage :- p-storage/IStorageClient]
  (condp = (format-extension fmt)
    :w (compare-less-then-float! fs ft coproc-storage)
    :d (throw (ex-info "Not implemented format" {:func "c-lt!"}))))

(s/defn ^:private branch-on!
  [{:keys [bin]} :- m.instruction/FRInstruction
   _ :- p-storage/IStorageClient
   _ :- p-storage/IStorageClient]
  (let [offset       (subs bin 16 32)
        decision-bit (a.number-base/bin->numeric (subs bin 15 16))
        branch-addr  (l.binary/signal-extend-32bits (str offset "00"))]
    (condp = decision-bit
      1 (when @db.coproc1/condition-flag
          (db.registers/sum-jump-addr! (a.number-base/bin->numeric branch-addr)))

      0 (when (not @db.coproc1/condition-flag)
          (db.registers/sum-jump-addr! (a.number-base/bin->numeric branch-addr)))

      :else (throw (ex-info "Invalid decision bit" {:bit decision-bit})))))

(s/def ^:private fr-table-by-func
  {"000110" {:str "mov" :action mov!}
   "100001" {:str "cvt.d" :action cvt-d!}
   "000000" {:str "add" :action add! :second-reg true}
   "000001" {:str "sub" :action sub! :second-reg true}
   "000011" {:str "div" :action div! :second-reg true}
   "100000" {:str "cvt.s" :action cvt-s!}
   "000010" {:str "mul" :action mul! :second-reg true}
   "111100" {:str "c.lt" :action c-lt! :mem-reg true}})

(s/def ^:private fr-table-by-fmt
  {"00000" {:str "mfc1" :action move-float-to-reg! :second-reg true :mem-reg true}
   "00100" {:str "mtc1" :action move-word-to-float! :second-reg true :mem-reg true}
   "01000" {:str "bc1" :action branch-on! :branch true}})

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
  [{:keys [funct fmt fd fs ft hex bin]} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [operation    (operation funct fmt)
        func-name    (:str operation)
        _assert      (assert (not (nil? func-name)) (str "Operation not found on fr-table\n func: " funct " fmt :" fmt " hex: " hex))
        second-reg?  (:second-reg operation)
        mem-reg?     (:mem-reg operation)
        branch?      (:branch operation)
        decision-bit (when branch? (a.number-base/bin->numeric (subs bin 15 16)))
        offset-value (when branch? (l.binary/bin->hex-str (subs bin 16 32)))
        fd-name      (when-not (or mem-reg? branch?) (db.coproc1/read-name! (a.number-base/bin->numeric fd) coproc-storage))
        fs-name      (when-not branch? (db.coproc1/read-name! (a.number-base/bin->numeric fs) coproc-storage))
        rt-name      (when second-reg? (db.registers/read-name! (a.number-base/bin->numeric ft) storage))]
    (if branch?
      (str func-name "." (condp = decision-bit 1 "t" 0 "f" :else "ERRO") " " offset-value)
      (str func-name " " (string/join ", " (remove nil? [rt-name fd-name fs-name]))))))

(s/defn execute!
  [{:keys [funct fmt] :as instruction} :- m.instruction/FRInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [func-fn (:action (operation funct fmt))]
    (func-fn instruction storage coproc-storage)))
