(ns isa-mips.adapters.file-input
  (:require [schema.core :as s]
            [isa-mips.models.action :as m.action]
            [clojure.string :as string]
            [isa-mips.adapters.number-base :as a.number-base]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream]))

(s/defn action->action-type :- m.action/Type
  [action :- s/Str]
  (s/validate m.action/Type (keyword action)))

(s/defn ^:private little-endian-instruction
  [byte-array begin]
  (->> (+ begin 4)
       (subvec byte-array begin)
       (reverse)))

(s/defn byte-array->binary-instruction-array :- [s/Str]
  [byte-array :- [Byte]]
  (for [i (range 0 (count byte-array) 4)]
    (->> (little-endian-instruction byte-array i)
         (map a.number-base/binary-string-zero-extend)
         string/join)))

(defn file->bytes
  "reference: https://clojuredocs.org/clojure.java.io/input-stream"
  [file-path section]
  (try
    (with-open [xin  (io/input-stream (str file-path "." section))
                xout (ByteArrayOutputStream.)]
      (io/copy xin xout)
      (.toByteArray xout))
    (catch Exception _ nil)))
