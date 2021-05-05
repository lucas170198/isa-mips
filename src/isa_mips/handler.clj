(ns isa-mips.handler
  (:require [isa-mips.controllers.data-section :as c.data-section]
            [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.logic.instructions :as l.instructions]
            [isa-mips.controllers.decode :as c.decode]
            [isa-mips.controllers.text-section :as c.text-section]
            [isa-mips.controllers.runner :as c.runner]
            [isa-mips.controllers.rodata-section :as c.rodata-section]))

(defmulti execute-action
          (fn [type _text _data _rodata _storage _coprcc-storage _memory] type))

(defmethod execute-action :decode
  [_ text _ _ storage coproc-storage _]
  (->> (a.file-input/byte-array->binary-instruction-array text)
       (map l.instructions/decode-binary-instruction)
       (c.decode/print-instructions! storage coproc-storage)))

(defmethod execute-action :run
  [_ text data rodata registers coproc-storage memory]
  (c.data-section/store-data-section! data memory)
  (c.rodata-section/store-rodata-section! rodata memory)
  (c.text-section/store-text-section!
   (a.file-input/byte-array->binary-instruction-array text)
   memory)
  (c.runner/run-program! registers coproc-storage memory))