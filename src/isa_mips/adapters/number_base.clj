(ns isa-mips.adapters.number-base
  (:require [schema.core :as s]
            [isa-mips.logic.binary :as l.binary]))

(s/defn bin->numeric
  [bin :- s/Str]
  (Integer/parseUnsignedInt bin 2))

(s/defn bin->float
  [bin :- s/Str]
  (-> (new BigInteger bin 2)
      (.intValue)
      (Float/intBitsToFloat)))

(s/defn float->bin
  [value :- Float]
  (-> (Float/floatToIntBits value)
      (Integer/toBinaryString)))

(s/defn double->bin
  [value :- Double]
  (-> (Double/doubleToLongBits value)
      (Long/toBinaryString)))

(s/defn bin->double
  [bin :- s/Str]
  (-> (new BigInteger bin 2)
      (.longValue)
      (Double/longBitsToDouble)))

(s/defn double->float
  [value]
  (.floatValue value))

(defn binary-string-zero-extend
  "Returns a binary representation of a byte value.
  reference: https://gist.github.com/benzap/7cda95aeaeecac12b5763a72ddb89310 "
  ([x]
   (binary-string-zero-extend x 8))
  ([x n]
   (let [s (Integer/toBinaryString x)
         c (count s)]
     (if (< c n)
       (str (apply str (repeat (- n c) "0")) s)
       (subs s (- (count s) n) (count s))))))

(defn binary-string-signal-extend
  ([x]
   (binary-string-signal-extend x 8))
  ([x n]
   (let [bin (Long/toBinaryString x)
         bin-sig (if (pos? x) ;putting the right signal
                   (str "0" bin)
                   bin)]

     (l.binary/signal-extend-nbits bin-sig n))))