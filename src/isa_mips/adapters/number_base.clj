(ns isa-mips.adapters.number-base
  (:require [schema.core :as s]))

(s/defn bin->numeric
  [bin :- s/Str]
  (Integer/parseUnsignedInt bin 2))

(defn binary-string
  "Returns a binary representation of a byte value.
  reference: https://gist.github.com/benzap/7cda95aeaeecac12b5763a72ddb89310 "
  ([x]
   (binary-string x 8))
  ([x n]
   (let [s (Integer/toBinaryString x)
         c (count s)]
     (if (< c n)
       (str (apply str (repeat (- n c) "0")) s)
       (subs s (- (count s) n) (count s))))))
