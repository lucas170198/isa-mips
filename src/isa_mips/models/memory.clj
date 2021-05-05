(ns isa-mips.models.memory
  (:require [schema.core :as s]
            [isa-mips.protocols.storage-client :as storage-client]))

(def data {:value                 s/Str
           (s/optional-key :name) s/Str})

(def reg-skeleton {:addr s/Int :meta data})

(def Registers [reg-skeleton])


(def level-type (s/enum :L1 :L1i :L1d :L2 :RAM))

(def MemConfig {:level                           level-type
                (s/optional-key :size)           s/Int
                (s/optional-key :assoc-param)    s/Int
                (s/optional-key :line-size)      s/Int
                (s/optional-key :next-level-ref) storage-client/IStorageClient})

(def memory-data {:value                  s/Str
                  (s/optional-key :set) s/Int
                  (s/optional-key :valid) s/Bool
                  (s/optional-key :dirty) s/Bool
                  (s/optional-key :tag)   s/Str})

(def memory-skeleton {:address s/Int :meta memory-data})

(def Memory [memory-skeleton])

(def DecodedAddress  {:byte-offset  s/Str
                      :block-offset s/Str
                      :index        s/Str
                      :tag          s/Str})
