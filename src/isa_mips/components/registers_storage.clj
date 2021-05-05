(ns isa-mips.components.registers-storage
  (:require [schema.core :as s]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.protocols.storage-client :as storage-client]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.components.db-commons :as db-commons]))

(s/defn ^:private register-class "Creates a class of registers with the same prefix"
  [prefix init-idx vector]
  (map-indexed (fn [idx _]
                 {:addr (+ idx init-idx)
                  :meta {:name  (str prefix idx)
                         :value (a.number-base/binary-string-zero-extend 0)}}) vector))

(defn ^:private get-by-name
  [storage name]
  (-> #(= (get-in % [:meta :name]) name)
      (filterv storage)
      first))

(defrecord RegistersMemory [storage]
  storage-client/StorageClient
  (read-value-by-name! [_this reg-name]
    (-> (db-commons/get-db storage) (get-by-name reg-name) (get-in [:meta :value])))

  (read-name! [_this address]
    (-> (db-commons/get-db storage) (db-commons/first-by-address address) (get-in [:meta :name])))

  (read-value! [_this address]
    (db-commons/read-value-from-storage storage address))

  (write-value! [_this address value]
    (db-commons/write-to-mem! storage address value)))

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
