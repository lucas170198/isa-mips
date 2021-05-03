(ns isa-mips.logic.binary
  (:require [schema.core :as s]))

(defn bin->complement-of-two-int
  [str]
  (let [num (Integer/parseInt str 2)]
    (if (> num 32767)
      (- num 65536)
      num)))

(s/defn signal-extend-nbits
  [bin :- s/Str
   n :- s/Int]
  (let [c (count bin)
        signal (subs bin 0 1)]
    (if (< c n)
      (str (apply str (repeat (- n c) signal)) bin)
      (subs bin (- c n) c))))

(s/defn zero-extend-nbits
  [bin :- s/Str
   n :- s/Int]
  (let [c (count bin)]
    (if (< c n)
      (str (apply str (repeat (- n c) "0")) bin)
      (subs bin (- c n) c))))

(s/defn zero-extend-32bits
  [bin :- s/Str]
  (zero-extend-nbits bin 32))

(s/defn signal-extend-32bits
  [bin :- s/Str]
  (signal-extend-nbits bin 32))

(defn sum
  [reg1 reg2]
  (+ (Integer/parseUnsignedInt  reg1 2) (Integer/parseUnsignedInt  reg2 2)))

(defn sub
  [reg1 reg2]
  (- (Integer/parseUnsignedInt  reg1 2) (Integer/parseUnsignedInt  reg2 2)))

(defn signed-sum
  [reg1 reg2]
  (+ (Integer/parseInt reg1 2) (Integer/parseInt reg2 2)))

(defn bin->hex-str
  [bin-str]
  (->> (Integer/parseUnsignedInt bin-str 2)
       Integer/toHexString
       (str " 0x")))
