(ns isa-mips.db.registers
  (:require [schema.core :as s]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.models.instruction :as m.instruction]
            [clojure.string :as str]
            [isa-mips.protocols.storage-client :as p-storage]
            [isa-mips.protocols.storage-client :as storage-client]))

(def pc-init 0x00400000)

(s/def pc (atom pc-init))

(s/def jump-addr "Store the jump address when a instructions is a jump one. This is for solve the branch delay problem"
  (atom nil))

(s/defn read-reg-value!
  [address :- s/Int
   registers-storage :- p-storage/IStorageClient]
  (storage-client/read-value! registers-storage address))

(s/defn read-value!
  [initial-address :- s/Int
   registers-storage :- p-storage/IStorageClient]
  (-> #(storage-client/read-value! registers-storage %)
      (mapv (range initial-address (+ initial-address 4)))
      reverse
      str/join))

(s/defn read-name!
  [address :- s/Int
   registers-storage :- p-storage/IStorageClient]
  (storage-client/read-name! registers-storage address))

(s/defn write-value! :- (s/maybe m.memory/Store)
  [address :- s/Int
   value :- m.instruction/fourBytesString
   registers-storage :- p-storage/IStorageClient]
  (when-not (= address 0)
    (storage-client/write-value! registers-storage address value)))

(s/defn read-value-by-name!
  [name :- s/Str
   register-storage :- p-storage/IStorageClient]
  (storage-client/read-value-by-name! register-storage name))

(s/defn inc-program-counter!
  []
  (swap! pc + 4))

(s/defn set-program-counter!
  [value :- s/Int]
  (reset! pc value))

(s/defn sum-jump-addr!
  [value :- s/Int]
  (reset! jump-addr (+ @pc value)))

(s/defn set-jump-addr!
  [value :- s/Int]
  (reset! jump-addr value))

(s/defn clear-jump-addr!
  []
  (reset! jump-addr nil))


