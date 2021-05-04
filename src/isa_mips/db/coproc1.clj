(ns isa-mips.db.coproc1
  (:require [schema.core :as s]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.models.instruction :as m.instruction]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.protocols.storage-client :as storage-client]))

(def hi (atom (a.number-base/binary-string-zero-extend 0)))

(def lo (atom (a.number-base/binary-string-zero-extend 0)))

(def condition-flag (atom false))

(defn set-condition-flag!
  []
  (reset! condition-flag true))

(defn reset-condition-flag!
  []
  (reset! condition-flag false))

(s/defn set-hi!
  [hi-value :- s/Str]
  (reset! hi hi-value))

(s/defn set-lo!
  [lo-value :- s/Str]
  (reset! lo lo-value))

(s/defn read-value-by-name! :- m.instruction/fourBytesString
  [name :- s/Str
   coproc1-storage :- p-storage/IStorageClient]
  (storage-client/read-value-by-name! coproc1-storage name))

(s/defn read-name!
  [address :- s/Int
   coproc1-storage :- p-storage/IStorageClient]
  (storage-client/read-name! coproc1-storage address))

(s/defn write-value! :- (s/maybe m.memory/Registers)
  [address :- s/Int
   value :- m.instruction/fourBytesString
   coproc1-storage :- p-storage/IStorageClient]
  (storage-client/write-value! coproc1-storage address value))

(s/defn read-value! :- m.instruction/fourBytesString
  [address :- s/Int
   coproc1-storage :- p-storage/IStorageClient]
  (storage-client/read-value! coproc1-storage address))

(s/defn write-double-on-memory!
  [addr :- s/Int
   value :- s/Str
   storage :- p-storage/IStorageClient]
  (write-value! (+ addr 1) (subs value 0 32) storage)
  (write-value! addr (subs value 32 64) storage))

(s/defn load-double-from-memory!
  [reg :- s/Str
   storage :- p-storage/IStorageClient]
  (let [lo-addr (a.number-base/bin->numeric reg)
        hi-addr (+ 1 lo-addr)]
    (str (read-value! hi-addr storage) (read-value! lo-addr storage))))
