(ns isa-mips.adapters.file-input
  (:require [clojure.java.io :as io]
            [schema.core :as s]
            [isa-mips.models.action :as m.action])
  (:import [java.io ByteArrayOutputStream]))

(defn file->bytes
  "reference: https://clojuredocs.org/clojure.java.io/input-stream"
  [file-path]
  (with-open [xin (io/input-stream file-path)
              xout (ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(s/defn action->action-type :- m.action/Type
  [action :- s/Str]
  (s/validate m.action/Type (keyword action)))
