(ns isa-mips.controllers.r-ops
  (:require [schema.core :as s]
            [clojure.string :as string]
            [isa-mips.db.registers :as db.registers]
            [isa-mips.logic.binary :as l.binary]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.db.coproc1 :as db.coproc1]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.models.instruction :as m.instruction]))

(s/defn ^:private add!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result  (l.binary/signed-sum rs-bin rt-bin)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private addu!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result  (l.binary/sum rs-bin rt-bin)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private subu!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result  (l.binary/sub rs-bin rt-bin)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private set-less-than!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result  (if (< (a.number-base/bin->numeric rs-bin)
                       (a.number-base/bin->numeric rt-bin)) 1 0)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-zero-extend result 32) storage)))

(s/defn ^:private jump-register!
  [_ :- s/Str
   rs :- s/Str
   _ :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [target-address (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)]
    (db.registers/set-jump-addr! (a.number-base/bin->numeric target-address))))

(s/defn ^:private shift-left!
  [rd :- s/Str
   _ :- s/Str
   rt :- s/Str
   shamt :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-left (a.number-base/bin->numeric rt-bin) shamt-value)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private shift-right!
  [rd :- s/Str
   _ :- s/Str
   rt :- s/Str
   shamt :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-right (a.number-base/bin->numeric rt-bin) shamt-value)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private r-jump-and-link!
  [rd :- s/Str
   rs :- s/Str
   _ :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr               (a.number-base/bin->numeric rd)
        jump-addr             (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        next-instruction-addr (+ @db.registers/pc 4)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-zero-extend next-instruction-addr 32) storage)
    (db.registers/set-jump-addr! (- (Integer/parseUnsignedInt jump-addr 2) 4))))

(s/defn ^:private or!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [destiny-reg (a.number-base/bin->numeric rd)
        rs-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result      (bit-or (a.number-base/bin->numeric rs-bin) (a.number-base/bin->numeric rt-bin))]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private xor!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [destiny-reg (a.number-base/bin->numeric rd)
        rs-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result      (bit-xor (a.number-base/bin->numeric rs-bin) (a.number-base/bin->numeric rt-bin))]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private div!
  [_ :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rs-bin     (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin     (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        rs-value   (a.number-base/bin->numeric rs-bin)
        rt-value   (a.number-base/bin->numeric rt-bin)
        div-result (/ rs-value rt-value)
        rem        (rem rs-value rt-value)]
    (db.coproc1/set-lo! (a.number-base/binary-string-signal-extend div-result 32))
    (db.coproc1/set-hi! (a.number-base/binary-string-signal-extend rem 32))))

(s/defn ^:private mfhi!
  [rd :- s/Str
   _ :- s/Str
   _ :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)]
    (db.registers/write-value! rd-addr @db.coproc1/hi storage)))

(s/defn ^:private mflo!
  [rd :- s/Str
   _ :- s/Str
   _ :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)]
    (db.registers/write-value! rd-addr @db.coproc1/lo storage)))

(s/defn ^:private mult!
  [_ :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rs-bin     (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin     (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        rs-value   (a.number-base/bin->numeric rs-bin)
        rt-value   (a.number-base/bin->numeric rt-bin)
        result     (* rs-value rt-value)
        result-bin (a.number-base/binary-string-signal-extend result 64)]
    (db.coproc1/set-hi! (subs result-bin 0 32))
    (db.coproc1/set-lo! (subs result-bin 32 64))))

(s/defn ^:private and!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [destiny-reg (a.number-base/bin->numeric rd)
        rs-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result      (bit-and (a.number-base/bin->numeric rs-bin) (a.number-base/bin->numeric rt-bin))]
    (db.registers/write-value! destiny-reg (a.number-base/binary-string-signal-extend result 32) storage)))

(s/defn ^:private set-less-than-u!
  [rd :- s/Str
   rs :- s/Str
   rt :- s/Str
   _ :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr (a.number-base/bin->numeric rd)
        rs-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rs) storage)
        rt-bin  (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        result  (if (< (a.number-base/bin->numeric rs-bin)
                       (a.number-base/bin->numeric rt-bin)) 1 0)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-zero-extend result 32) storage)))

(s/defn ^:private shift-right-arithmetic!
  [rd :- s/Str
   _ :- s/Str
   rt :- s/Str
   shamt :- s/Str
   storage :- p-storage/IStorageClient
   _]
  (let [rd-addr     (a.number-base/bin->numeric rd)
        rt-bin      (db.registers/read-reg-value! (a.number-base/bin->numeric rt) storage)
        shamt-value (a.number-base/bin->numeric shamt)
        result      (bit-shift-right (a.number-base/bin->numeric rt-bin) shamt-value)]
    (db.registers/write-value! rd-addr (a.number-base/binary-string-signal-extend result 32) storage)))

(s/def r-table
  {"100000" {:str "add" :action add!}
   "100001" {:str "addu" :action addu! :unsigned true}
   "101010" {:str "slt" :action set-less-than!}
   "001000" {:str "jr" :action jump-register! :hide-destiny-reg true :hide-second-reg true}
   "000000" {:str "sll" :action shift-left! :shamt true}
   "000010" {:str "srl" :action shift-right! :shamt true}
   "001001" {:str "jalr" :action r-jump-and-link! :hide-second-reg true}
   "011000" {:str "mult" :action mult! :hide-destiny-reg true}
   "010010" {:str "mflo" :action mflo! :hide-second-reg true :hide-first-reg true}
   "010000" {:str "mfhi" :action mfhi! :hide-second-reg true :hide-first-reg true}
   "011010" {:str "div" :action div! :hide-destiny-reg true}
   "100101" {:str "or" :action or!}
   "001101" {:str "break" :action (constantly nil) :hide-destiny-reg true :hide-second-reg true :hide-first-reg true}
   "100110" {:str "xor" :action xor!}
   "100100" {:str "and" :action and!}
   "101011" {:str "sltu" :action set-less-than-u!}
   "100011" {:str "subu" :action subu!}
   "000011" {:str "sra" :action shift-right-arithmetic!}})

(s/defn operation-str! :- s/Str
  [{func :funct
    destiny-reg :rd
    first-reg :rs
    second-reg :rt
    shamt :shamt} :- m.instruction/RInstruction
   storage :- p-storage/IStorageClient]
  (let [operation        (get r-table func)
        func-name        (:str operation)
        _assert          (assert (not (nil? operation)) (str "Operation not found on r-table\n func: " func))
        shamt?           (:shamt operation)
        destiny-reg-name (when-not (:hide-destiny-reg operation) (db.registers/read-name! (a.number-base/bin->numeric destiny-reg) storage))
        first-reg-name   (when-not (or shamt? (:hide-first-reg operation))
                           (db.registers/read-name! (a.number-base/bin->numeric first-reg) storage))
        shamt            (when shamt? (a.number-base/bin->numeric shamt))
        second-name      (when-not (:hide-second-reg operation)
                           (db.registers/read-name! (a.number-base/bin->numeric second-reg) storage))]
    (str func-name " " (string/join ", " (remove nil? [destiny-reg-name first-reg-name second-name shamt])))))

(s/defn execute!
  [{func        :funct
    destiny-reg :rd
    first-reg   :rs
    second-reg  :rt
    shamt       :shamt} :- m.instruction/RInstruction
   storage :- p-storage/IStorageClient
   coproc-storage :- p-storage/IStorageClient]
  (let [func-fn (get-in r-table [func :action])]
    (func-fn destiny-reg first-reg second-reg shamt storage coproc-storage)))


