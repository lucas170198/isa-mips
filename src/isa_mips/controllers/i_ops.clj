(ns isa-mips.controllers.i-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.db.memory :as db.memory]
            [isa-mips.protocols.logger :as p-logger]))

(s/defn ^:private addi!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [destiny-reg      (a.number-base/bin->numeric destiny-reg)
        reg-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        immediate-signal (l.binary/signal-extend-32bits immediate)
        result           (l.binary/signed-sum reg-bin immediate-signal)]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private addiu!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [destiny-reg      (a.number-base/bin->numeric destiny-reg)
        reg-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        immediate-signal (l.binary/signal-extend-32bits immediate)
        result           (l.binary/sum reg-bin immediate-signal)]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private ori!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [destiny-reg     (a.number-base/bin->numeric destiny-reg)
        immediate-value (a.number-base/bin->numeric (l.binary/zero-extend-32bits immediate))
        reg-bin         (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        result          (bit-or immediate-value (a.number-base/bin->numeric reg-bin))]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private andi
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [destiny-reg     (a.number-base/bin->numeric destiny-reg)
        immediate-value (a.number-base/bin->numeric (l.binary/zero-extend-32bits immediate))
        reg-bin         (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        result          (bit-and immediate-value (a.number-base/bin->numeric reg-bin))]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private lui!
  [destiny-reg :- s/Str
   _reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [destiny-reg (a.number-base/bin->numeric destiny-reg)
        result      (str immediate (apply str (repeat 16 "0")))]
    (db.registers/write-value! destiny-reg result storage)))

(s/defn ^:private branch-not-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [rt-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric destiny-reg) storage)
        rs-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        branch-addr     (l.binary/signal-extend-32bits (str immediate "00"))
        immediate-value (a.number-base/bin->numeric branch-addr)]
    (when (not= (a.number-base/bin->numeric rt-bin) (a.number-base/bin->numeric rs-bin))
      (db.registers/sum-jump-addr! immediate-value))))

(s/defn ^:private branch-equal!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [rt-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric destiny-reg) storage)
        rs-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        branch-addr     (l.binary/signal-extend-32bits (str immediate "00"))
        immediate-value (a.number-base/bin->numeric branch-addr)]
    (when (= (a.number-base/bin->numeric rt-bin) (a.number-base/bin->numeric rs-bin))
      (db.registers/sum-jump-addr! immediate-value))))

(s/defn ^:private load-word!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        offset       (l.binary/signal-extend-32bits immediate)
        reg-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        target-addr  (l.binary/sum reg-bin offset)
        memory-value (db.memory/read-value! target-addr memory tracer)]
    (db.registers/write-value! destiny-addr memory-value storage)))

(s/defn ^:private store-word!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [destiny-value (db.registers/read-reg-value! (a.number-base/bin->numeric destiny-reg) storage)
        offset        (l.binary/signal-extend-32bits immediate)
        reg-bin       (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        target-addr   (l.binary/sum reg-bin offset)]
    (db.memory/write-value! target-addr destiny-value memory tracer)))

(s/defn ^:private imm-set-less-then!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [immediate-value (a.number-base/bin->numeric (l.binary/signal-extend-32bits immediate))
        reg-bin         (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        destiny-reg     (a.number-base/bin->numeric destiny-reg)
        result          (if (< (a.number-base/bin->numeric reg-bin)
                               immediate-value) 1 0)]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-zero-extend result 32) storage)))

(s/defn ^:private load-word-float!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [destiny-addr (a.number-base/bin->numeric destiny-reg)
        offset       (l.binary/signal-extend-32bits immediate)
        reg-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        target-addr  (l.binary/sum reg-bin offset)
        memory-value (db.memory/read-value! target-addr memory tracer)]
    (db.coproc1/write-value! destiny-addr memory-value coproc-storage)))

(s/defn ^:private load-word-double!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [lo-destiny-addr (a.number-base/bin->numeric destiny-reg)
        hi-destiny-addr (+ lo-destiny-addr 1)
        offset          (l.binary/signal-extend-32bits immediate)
        reg-bin         (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        lo-addr         (l.binary/sum reg-bin offset)
        lo-memory-value (db.memory/read-value! lo-addr memory tracer)
        hi-memory-value (db.memory/read-value! (+ lo-addr 4) memory tracer)]
    (db.coproc1/write-value! lo-destiny-addr lo-memory-value coproc-storage)
    (db.coproc1/write-value! hi-destiny-addr hi-memory-value coproc-storage)))

(s/defn ^:private load-byte!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [destiny-addr  (a.number-base/bin->numeric destiny-reg)
        offset        (l.binary/signal-extend-32bits immediate)
        reg-bin       (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        target-addr   (l.binary/sum reg-bin offset)
        destiny-value (db.memory/read-value! target-addr memory tracer)]
    (db.registers/write-value! destiny-addr destiny-value storage)))


(s/defn ^:private branch-equal-less-zero!
  [_ :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [rs-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        branch-addr     (l.binary/signal-extend-32bits (str immediate "00"))
        immediate-value (a.number-base/bin->numeric branch-addr)]
    (when (<= (a.number-base/bin->numeric rs-bin) 0)
      (db.registers/sum-jump-addr! immediate-value))))

(s/defn ^:private branch-on-greater-then-zero!
  [_ :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   _
   _
   _]
  (let [rs-bin          (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        branch-addr     (l.binary/signal-extend-32bits (str immediate "00"))
        immediate-value (a.number-base/bin->numeric branch-addr)]
    (when (>= (a.number-base/bin->numeric rs-bin) 0)
      (db.registers/sum-jump-addr! immediate-value))))

(s/defn ^:private store-word-float!
  [destiny-reg :- s/Str
   reg :- s/Str
   immediate :- s/Str
   storage :- p-storage/IStorageClient
   coproc1 :- p-storage/IStorageClient
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [destiny-value (db.coproc1/read-value! (a.number-base/bin->numeric destiny-reg) coproc1)
        reg-bin       (db.registers/read-reg-value! (a.number-base/bin->numeric reg) storage)
        target-addr   (l.binary/sum reg-bin (l.binary/signal-extend-32bits immediate))]
    (db.memory/write-value! target-addr destiny-value memory tracer)))

(s/def i-table
  {"001000" {:str "addi" :action addi!}
   "001001" {:str "addiu" :action addiu! :unsigned true}
   "001101" {:str "ori" :action ori!}
   "001100" {:str "andi" :action andi}
   "001111" {:str "lui" :action lui! :hide-first true}
   "000100" {:str "beq" :action branch-equal!}
   "000101" {:str "bne" :action branch-not-equal!}
   "100011" {:str "lw" :action load-word! :memory-op true}
   "101011" {:str "sw" :action store-word! :memory-op true}
   "001010" {:str "slti" :action imm-set-less-then!}
   "110001" {:str "lwc1" :action load-word-float! :coproc1 true :memory-op true}
   "110101" {:str "ldc1" :action load-word-double! :coproc1 true :memory-op true}
   "100000" {:str "lb" :action load-byte! :memory-op true}
   "000110" {:str "blez" :action branch-equal-less-zero!}
   "00001"  {:str "bgez" :action branch-on-greater-then-zero! :hide-first true}
   "111001" {:str "swc1" :action store-word-float! :memory-op true}})

(s/defn operation-str! :- s/Str
  [{op-code :op
    destiny-reg :rt
    reg :rs
    immediate :immediate} :- m.instruction/IInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [operation        (get i-table op-code)
        func-name        (:str operation)
        _assert          (assert (not (nil? func-name)) (str "Operation not found on i-table: " op-code))
        num-destiny-reg  (a.number-base/bin->numeric destiny-reg)
        destiny-reg-name (when-not (:hide-first operation) (if (:coproc1 operation) (db.coproc1/read-name! num-destiny-reg coproc-storage)
                                                                                (db.registers/read-name! num-destiny-reg storage)))
        reg-name         (db.registers/read-name! (a.number-base/bin->numeric reg) storage)
        unsigned?        (:unsigned operation)
        memory-op?       (:memory-op operation)
        immediate-dec    (if unsigned? (Integer/parseUnsignedInt immediate 2) (l.binary/bin->complement-of-two-int immediate))]
    (if memory-op?
      (str func-name " " destiny-reg-name ", " (l.binary/bin->hex-str immediate) "(" reg-name ")")
      (str func-name " " (string/join ", " (remove nil? [destiny-reg-name reg-name immediate-dec]))))))

(s/defn execute!
  [{op-code     :op
    reg         :rs
    destiny-reg :rt
    immediate   :immediate} :- m.instruction/IInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient
   memory :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (let [func-fn (get-in i-table [op-code :action])]
    (func-fn destiny-reg reg immediate storage coproc-storage memory tracer)))
