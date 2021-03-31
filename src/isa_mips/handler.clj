(ns isa-mips.handler
  (:require [isa-mips.controllers.data-section :as c.data-section]
            [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.logic.instructions :as l.instructions]
            [isa-mips.controllers.decode :as c.decode]
            [isa-mips.controllers.text-section :as c.text-section]
            [isa-mips.controllers.runner :as c.runner]))

(defmulti execute-action (fn [type _text _data] type))

(defmethod execute-action :decode
  [_ text _]
  (->> (a.file-input/byte-array->binary-instruction-array text)
       (map l.instructions/decode-binary-instruction)
       c.decode/print-instructions!))

(defmethod execute-action :run
  [_ text data]
  (c.data-section/store-data-section! data)
  (c.text-section/store-text-section!
   (a.file-input/byte-array->binary-instruction-array text))
  (c.runner/run-program!))