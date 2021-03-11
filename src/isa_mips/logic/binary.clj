(ns isa-mips.logic.binary)

(defn bin->complement-of-two-int
  [str]
  (let [num (Integer/parseInt str 2)]
    (if (> num 32767)
      (- num 65536)
      num)))

(defn unsigned-sum
  [reg1 reg2]
  (+ (Integer/parseUnsignedInt reg1 2) (Integer/parseUnsignedInt reg2 2)))

(defn signed-sum
  [reg1 reg2]
  (+ (Integer/parseInt reg1 2) (Integer/parseInt reg2 2)))

(defn bin->hex-str
  [bin-str]
  (->> (Integer/parseInt bin-str 2)
       Integer/toHexString
       (str " 0x")))
