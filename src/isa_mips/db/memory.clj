(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [isa-mips.protocols.storage-client :as p-storage]
            [clojure.string :as str]))

(s/defn read-instruction!
  [address :- s/Int
   memory-storage :- p-storage/IStorageClient]
  (p-storage/read-value! memory-storage address))

(s/defn write-instruction!
  [address :- s/Int
   value :- s/Str
   memory-storage :- p-storage/IStorageClient]
  (p-storage/write-value! memory-storage address value true))

(s/defn read-value!
  [initial-address :- s/Int
   registers-storage :- p-storage/IStorageClient]
  (-> #(p-storage/read-value! registers-storage %)
      (mapv (range initial-address (+ initial-address 4)))
      reverse
      str/join))

(s/defn read-byte!
  [address :- s/Int
   registers-storage :- p-storage/IStorageClient]
  (p-storage/read-value! registers-storage address))

(s/defn write-value!
  [address :- s/Int
   value :- s/Str
   memory-storage :- p-storage/IStorageClient]
  (p-storage/write-value! memory-storage address value))


