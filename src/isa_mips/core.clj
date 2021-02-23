(ns isa-mips.core
  (:require [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.handler :as handler]))

(defn -main
  [& args]
  (let [action-type  (a.file-input/action->action-type (first args))
        text-section (a.file-input/file->bytes (last args) "text")
        data-section (a.file-input/file->bytes (last args) "data")]
    (handler/execute-action action-type text-section data-section)))
