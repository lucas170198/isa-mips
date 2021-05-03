(ns isa-mips.protocols.storage-client
  (:require [schema.core :as s]))

(defprotocol StorageClient "Protocol to registers manipulation"
  (read-value-by-name! [storage reg-name])
  (read-value! [storage address])
  (read-name! [storage address])
  (write-value! [storage address value]))

(def IStorageClient (s/protocol StorageClient))
