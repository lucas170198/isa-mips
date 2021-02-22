(ns isa-mips.handler
  (:require [schema.core :as s]))

(defmulti execute-action (fn [type _file] type))

(defmethod execute-action :decode
  [_ file]
  (prn "Decode action" (nth file 2)))

(defmethod execute-action :run
  [_ file]
  (prn "Run action" (nth file 2)))