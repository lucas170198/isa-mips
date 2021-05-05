(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.protocols.logger :as p-logger]
            [isa-mips.logic.memory :as logic.memory]
            [isa-mips.models.memory :as models.memory]
            [isa-mips.adapters.number-base :as a.number-base]))

(s/defn ^:private logged-index
  [initial-address :- s/Int
   {:keys [level] :as config} :- models.memory/MemConfig]
  (let [bit-str-addr (a.number-base/binary-string-signal-extend initial-address 32)]
    (if (= level :RAM)
      bit-str-addr
      (:index (logic.memory/decode-address bit-str-addr config)))))

(s/defn write-instruction!
  [address :- s/Int
   value :- s/Str
   memory-storage :- p-storage/IStorageClient]
  (p-storage/write-value! memory-storage address value true))

(s/defn read-value!
  [initial-address :- s/Int
   mem-storage :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (p-logger/log-read! tracer initial-address (logged-index initial-address (.config_map mem-storage)) false)
  (p-storage/read-value! mem-storage initial-address))

(s/defn read-instruction!
  [initial-address :- s/Int
   mem-storage :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (p-logger/log-read! tracer initial-address (logged-index initial-address (.config_map mem-storage)) true)
  (p-storage/read-value! mem-storage initial-address true))

(s/defn read-byte!
  [address :- s/Int
   mem-storage :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (p-logger/log-read! tracer address (logged-index address (.config_map mem-storage)) false)
  (p-storage/read-byte! mem-storage address))

(s/defn write-value!
  [address :- s/Int
   value :- s/Str
   memory-storage :- p-storage/IStorageClient
   tracer :- p-logger/ILogger]
  (p-logger/log-write! tracer address (logged-index address (.config_map memory-storage)))
  (p-storage/write-value! memory-storage address value))


