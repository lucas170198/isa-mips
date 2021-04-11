(ns isa-mips.models.instruction
  (:require [schema.core :as s]
            [schema.experimental.abstract-map :as abstract-map]))

(defn bitString [size]
  (s/constrained s/Str #(= (count %) size)))

(s/defschema fourBytesString (bitString 32))

(s/defschema BaseInstruction
  (abstract-map/abstract-map-schema
   :format
   {(s/optional-key :op) (bitString 6)}))

(abstract-map/extend-schema RInstruction BaseInstruction [:R] {:rs    (bitString 5)
                                                               :rt    (bitString 5)
                                                               :rd    (bitString 5)
                                                               :shamt (bitString 5)
                                                               :funct (bitString 6)})

(abstract-map/extend-schema IInstruction BaseInstruction [:I] {:rs        (bitString 5)
                                                               :rt        (bitString 5)
                                                               :immediate (bitString 16)})

(abstract-map/extend-schema JInstruction BaseInstruction [:J] {:target-address (bitString 26)})

(abstract-map/extend-schema Syscall BaseInstruction [:SYSCALL] {})

(abstract-map/extend-schema Nop BaseInstruction [:NOP] {})

(abstract-map/extend-schema FRInstruction BaseInstruction [:FR] {:fmt   (bitString 5)
                                                                :ft    (bitString 5)
                                                                :fs    (bitString 5)
                                                                :fd    (bitString 5)
                                                                :funct (bitString 6)})
