(ns isa-mips.logic.binary)

(defn bin->complement-of-two-int
  [str]
  (let [num (Integer/parseInt str 2)]
    (if (> num 32767)
      (- num 65536)
      num)))

(defn sum
  [reg1 reg2]
  (+ (Long/parseLong  reg1 2) (Long/parseLong  reg2 2)))

(defn bin->hex-str
  [bin-str]
  (->> (Long/parseLong bin-str 2)
       Integer/toHexString
       (str " 0x")))
