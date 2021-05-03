(ns isa-mips.models.memory
  (:require [schema.core :as s]))

(def data {:value                 s/Str
           (s/optional-key :name) s/Str})

(def reg-skeleton {:addr s/Int :meta data})

(def Registers [reg-skeleton])

(def cache-type (s/enum :unified :split))

(def cache-config-skeleton {:type cache-type :size s/Int :assoc-param s/Int :line-size s/Int})

(def CacheConfig {:levels s/Int
                  :data   [cache-config-skeleton]})

(def cache-skeleton {:valid s/Bool
                     :dirty s/Bool
                     :tag   s/Int
                     :data  s/Str
                     :meta CacheConfig})

(def CacheSet {s/Int cache-skeleton})

(def CacheTable [CacheSet])

(def Memory {s/Int s/Str})

;Conf 2:
; L1: U-cache -> 32 linhas / 1 bloco [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}]

; Conf 5:
; L1 : I-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L1 : D-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)


; Conf 5:
; L1 : I-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L1 : D-cache -> 4 linhas / 4 blocos [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)} ... {index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}] (size 4)
; L2:  U-cache -> 8 linhas / 8 bloco [{index: 0 - 31 (5 bits), valid (bool), dirty (bool), tag (27 bits)}]
