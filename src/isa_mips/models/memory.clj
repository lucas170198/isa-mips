(ns isa-mips.models.memory
  (:require [schema.core :as s]
            [isa-mips.protocols.storage-client :as storage-client]))

(def data {:value                 s/Str
           (s/optional-key :name) s/Str})

(def reg-skeleton {:addr s/Int :meta data})

(def Registers [reg-skeleton])

(def cache-type (s/enum :unified :split :main))

(def MemConfig {:level                           s/Int
                :type                            cache-type
                (s/optional-key :size)           s/Int
                (s/optional-key :assoc-param)    s/Int
                (s/optional-key :line-size)      s/Int
                (s/optional-key :next-level-ref) storage-client/IStorageClient})

(def memory-data {:value                  s/Str
                  (s/optional-key :block) s/Int
                  (s/optional-key :valid) s/Bool
                  (s/optional-key :dirty) s/Bool
                  (s/optional-key :tag)   s/Str})

(def memory-skeleton {:address s/Int :meta memory-data})

(def Memory [memory-skeleton])

;Conf 2:
; L1: U-cache -> 32 linhas / 1 bloco [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}]

; Conf 5:
; L1 : I-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L1 : D-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)


; Conf 5:
; L1 : I-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L1 : D-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L2:  U-cache -> 8 linhas / 8 bloco [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}]
