(ns isa-mips.components.memory-storage
  (:require [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.protocols.storage-client :as storage-client]
            [isa-mips.protocols.logger :as logger]
            [isa-mips.logic.memory :as l.memory]
            [isa-mips.components.db-commons :as db-commons]
            [isa-mips.db.stats :as db.stats]
            [clojure.string :as str]))

(defn ^:private write-to-cache!
  [storage index value set tag]
  (swap! storage conj {:addr index
                       :meta {:value value :set set :tag tag :dirty false :valid true}}))

(defn- hit!
  [mem cache-data block-offset]
  (db.stats/hit-memory-by-level! (:level (.config_map mem)))
  (let [numeric-block-offset (a.number-base/bin->numeric block-offset)
        word-len             32
        word-start           (* numeric-block-offset word-len)]
    (subs cache-data word-start (+ word-len word-start))))

(defn- miss!
  [mem address {:keys [index set tag]}]
  (db.stats/miss-memory-by-level! (:level (.config_map mem)))
  (let [memory-value (storage-client/read-value! (:next-level-ref (.config_map mem)) address)]
    (write-to-cache! (.storage mem) index memory-value set tag)
    memory-value))


; READ BYTE
(defmulti read-byte-mem! (fn [mem _ _] (:level (.config_map mem))))

(defmethod read-byte-mem! :RAM
  [mem address _]
  (db.stats/hit-memory-by-level! :RAM)
  (db-commons/read-value-from-storage (.storage mem) address))


; READ 4 BYTES VALUE

(defmulti read-value-mem! (fn [mem _ _] (:level (.config_map mem))))

(defmethod read-value-mem! :RAM
  [mem address _]
  (db.stats/hit-memory-by-level! :RAM)
  (-> #(db-commons/read-value-from-storage (.storage mem) %)
      (mapv (range address (+ address 4)))
      reverse
      str/join))

(defmethod read-value-mem! :L1
  [mem address _]
  (let [address-bit-string (a.number-base/binary-string-zero-extend address 32)
        {:keys [index tag block-offset] :as decoded} (l.memory/decode-address address-bit-string (.config_map mem))
        cache-block        (db-commons/get-from-storage (.storage mem) (a.number-base/bin->numeric index))
        cache-line         (l.memory/search-by-tag cache-block tag)]
    (if (nil? cache-line)
      (miss! mem address decoded)
      (hit! mem (get-in cache-line [:meta :value]) block-offset))))


; WRITE 4 BYTES VALUE

(defmulti write-value-mem! (fn [mem _storage _address _value _instruction?]
                             (:level (.config_map mem))))

(defmethod write-value-mem! :RAM
  [_mem storage address value _instruction?]
  (db-commons/write-to-mem! storage address value))

(defmethod write-value-mem! :L1
  [mem _storage address value instruction?]
  (storage-client/write-value! (:next-level-ref (.config_map mem)) address value instruction?))



; Record
(defrecord Memory [storage config-map]
  storage-client/StorageClient

  (read-value! [this address]
    (storage-client/read-value! this address false))

  (read-value! [this address instruction?]
    (read-value-mem! this address instruction?))

  (read-byte! [this address]
    (storage-client/read-byte! this address false))

  (read-byte! [this address instruction?]
    (read-byte-mem! this address instruction?))

  (write-value! [this address value]
    (storage-client/write-value! this address value false))

  (write-value! [this address value instruction?]
    (write-value-mem! this storage address value instruction?)))

(defn ^:primary empty-mem [] (atom []))

(def ^:private main-memory-config {:level :RAM})

(def ^:private l1-unified-cache-config
  {:level          :L1
   :assoc-param    1
   :size           1024
   :line-size      32
   :next-level-ref (->Memory (empty-mem) main-memory-config)})

(defn ^:private mem-config
  [config-mode]
  (condp = config-mode
    1 main-memory-config

    2 l1-unified-cache-config

    :else (println "deu ruim")))

(defn new-memory
  [config-mode]
  (let [config-map (mem-config config-mode)]
    (->Memory (empty-mem) config-map)))
