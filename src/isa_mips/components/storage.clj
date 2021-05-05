(ns isa-mips.components.storage
  (:require [schema.core :as s]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.protocols.storage-client :as storage-client]
            [isa-mips.models.memory :as m.memory]))

(s/defn ^:private register-class "Creates a class of registers with the same prefix"
  [prefix init-idx vector]
  (map-indexed (fn [idx _]
                 {:addr (+ idx init-idx)
                  :meta {:name  (str prefix idx)
                         :value (a.number-base/binary-string-zero-extend 0)}}) vector))

(defn ^:private get-by-address
  [storage address]
  (-> #(= (:addr %) address)
      (filterv storage)
      first))

(defn ^:private get-by-name
  [storage name]
  (-> #(= (get-in % [:meta :name]) name)
      (filterv storage)
      first))

(defn ^:private update-if
  "Update a element that matches with the pred"
  [coll value keys pred-fn]
  (mapv #(if (pred-fn %)
           (assoc-in % keys value) %) coll))

(defn ^:private get-db
  [storage]
  @storage)

(defn ^:private read-from-storage
  [storage address]
  (-> (get-db storage)
      (get-by-address address)
      (get-in [:meta :value])))

(defn ^:private write-to-storage!
  [storage address value]
  (if (nil? (get-by-address @storage address))
    (swap! storage conj {:addr address :meta {:value value}})
    (reset! storage (update-if (get-db storage) value [:meta :value] #(= (:addr %) address)))))

(defrecord RegistersMemory [storage]
  storage-client/StorageClient
  (read-value-by-name! [_this reg-name]
    (-> (get-db storage) (get-by-name reg-name) (get-in [:meta :value])))

  (read-name! [_this address]
    (-> (get-db storage) (get-by-address address) (get-in [:meta :name])))

  (read-value! [_this address]
    (read-from-storage storage address))

  (write-value! [_this address value]
    (write-to-storage! storage address value)))

(defn new-register-storage! []
  (let [pointers (list {:addr 28 :meta {:name "$gp" :value (a.number-base/binary-string-zero-extend 0x10008000 32)}}
                       {:addr 29 :meta {:name "$sp" :value (a.number-base/binary-string-zero-extend 0x7fffeffc 32)}}
                       {:addr 30 :meta {:name "$fp" :value (a.number-base/binary-string-zero-extend 0 32)}}
                       {:addr 31 :meta {:name "$ra" :value (a.number-base/binary-string-zero-extend 0 32)}})
        statics  (list {:addr 0 :meta {:name "$zero" :value (a.number-base/binary-string-zero-extend 0 32)}}
                       {:addr 1 :meta {:name "$at" :value (a.number-base/binary-string-zero-extend 0 32)}})
        v-reg    (register-class "$v" 2 (range 2))
        a-reg    (register-class "$a" 4 (range 4))
        temp-reg (register-class "$t" 8 (range 8))
        s-reg    (register-class "$s" 16 (range 8))
        t-reg    (register-class "$t" 24 (range 8 9))
        k-reg    (register-class "$k" 26 (range 2))]
    (->> (concat statics v-reg a-reg t-reg s-reg temp-reg k-reg pointers)
         vec
         (s/validate m.memory/Registers)
         (atom)
         ->RegistersMemory)))

(defn new-coproc1-storage!
  []
  (->> (vec (register-class "$f" 0 (range 32)))
       (s/validate m.memory/Registers)
       (atom)
       ->RegistersMemory))

(defn- mem-index
  [number-of-lines address]
  (mod address number-of-lines))

(defrecord Memory [storage config-map]
  storage-client/StorageClient

  (read-value! [_this address]
    (if (= (:type config-map :main))
      (read-from-storage storage address)))

  (write-value! [this address value]
    (storage-client/write-value! this address value false))

  (write-value! [_this address value instruction?]
    (if (or (= (:type config-map :main)) (not instruction?))
      (write-to-storage! storage address value)
      (storage-client/write-value! (:next-level-ref config-map) address value instruction?))))

(defn ^:primary empty-mem [] (atom []))

(def ^:private main-memory-config {:level 0 :type :main})

(def ^:private l1-cache-config
  {:level 1 :type :unified :assoc-param 1 :size 1024 :line-size 32
   :next-level-ref (->Memory (empty-mem) main-memory-config)})

(defn ^:private mem-config
  [config-mode]
  (condp = config-mode
    1 main-memory-config

    2 l1-cache-config

    :else (println "deu ruim")))

(defn new-memory
  [config-mode]
  (let [config-map (mem-config config-mode)]
    (->Memory (empty-mem) config-map)))


