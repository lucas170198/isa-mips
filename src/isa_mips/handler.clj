(ns isa-mips.handler
  (:require [isa-mips.controllers.data-section :as c.data-section]
            [isa-mips.adapters.file-input :as a.file-input]))

(defmulti execute-action (fn [type _text _data] type))

(defmethod execute-action :decode
  [_ text data]
  (println "Decode action\nText size: " (count text) "\nData size: " (count data))
  (prn (a.file-input/byte-array->binary-instruction-array text)))

(defmethod execute-action :run
  [_ text data]
  (c.data-section/store-data-section! data)
  (prn (isa-mips.db.memory/read-value! 268500992)))