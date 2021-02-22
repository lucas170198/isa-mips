(ns isa-mips.core
  (:require [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.handler :as handler]))


(defn -main
  [& args]
  (let [action-type (a.file-input/action->action-type (first args))
        file        (a.file-input/file->bytes (last args))]
    (handler/execute-action action-type file)))
