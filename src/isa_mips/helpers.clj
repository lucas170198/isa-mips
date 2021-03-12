(ns isa-mips.helpers
  (:require [clojure.java.io :as io]
            [schema.core :as s])
  (:import [java.io ByteArrayOutputStream]))

(defn binary-string
  "Returns a binary representation of a byte value.
  reference: https://gist.github.com/benzap/7cda95aeaeecac12b5763a72ddb89310 "
  ([x]
   (binary-string x 8))
  ([x n]
   (let [s (Long/toBinaryString x)
         c (count s)]
     (if (< c n)
       (str (apply str (repeat (- n c) "0")) s)
       (subs s (- (count s) n) (count s))))))

(defn register-class
  [prefix init-idx vector]
  (map-indexed (fn [idx _]
                 {:addr (+ idx init-idx)
                  :meta {:name  (str prefix idx)
                         :value (binary-string 0)}}) vector))

(defn file->bytes
  "reference: https://clojuredocs.org/clojure.java.io/input-stream"
  [file-path section]
  (with-open [xin  (io/input-stream (str file-path "." section))
              xout (ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))
