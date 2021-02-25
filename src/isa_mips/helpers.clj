(ns isa-mips.helpers
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream]))

(defn register-class
  [prefix init-idx vector]
  (into {} (map-indexed (fn [idx _]
                          {(+ idx init-idx) {:name  (str prefix idx)
                                             :value 0}}) vector)))
(defn binary-string
  "Returns a binary representation of a byte value.
  reference: https://gist.github.com/benzap/7cda95aeaeecac12b5763a72ddb89310 "
  [x]
  (let [s (Integer/toBinaryString x)
        c (count s)]
    (if (< c 8)
      (str (apply str (repeat (- 8 c) "0")) s)
      (subs s (- (count s) 8) (count s)))))

(defn file->bytes
  "reference: https://clojuredocs.org/clojure.java.io/input-stream"
  [file-path section]
  (with-open [xin  (io/input-stream (str file-path "." section))
              xout (ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))
