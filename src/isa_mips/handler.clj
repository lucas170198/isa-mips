(ns isa-mips.handler
  (:require [schema.core :as s]))

(defmulti execute-action (fn [type _text _data] type))

(defmethod execute-action :decode
  [_ text data]
  (println "Decode action\nText size: " (count text) "\nData size: " (count data))
  (prn (nth data 3)))

(defmethod execute-action :run
  [_ text data]
  (prn "Run action" (nth text 2)))