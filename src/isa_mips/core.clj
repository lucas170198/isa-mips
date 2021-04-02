(ns isa-mips.core
  (:gen-class)
  (:require [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.handler :as handler]))

(defn -main
  [& args]
  (let [action-type    (a.file-input/action->action-type (first args))
        text-section   (a.file-input/file->bytes (last args) "text")
        data-section   (a.file-input/file->bytes (last args) "data")
        rodata-section (a.file-input/file->bytes (last args) "rodata")]
    (handler/execute-action action-type (vec text-section) (vec data-section) (vec rodata-section))))
