(ns isa-mips.logic.binary)

(defn unsigned-sum
  [reg1 reg2]
  (+ (Integer/parseUnsignedInt reg1 2) (Integer/parseUnsignedInt reg2 2)))

(defn signed-sum
  [reg1 reg2]
  (+ (Integer/parseInt reg1 2) (Integer/parseInt reg2 2)))
