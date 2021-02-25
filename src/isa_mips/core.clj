(ns isa-mips.core
  (:require [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.helpers :as helpers]
            [isa-mips.handler :as handler]))

(defn -main
  [& args]
  (let [action-type  (a.file-input/action->action-type (first args))
        text-section (helpers/file->bytes (last args) "text")
        data-section (helpers/file->bytes (last args) "data")]
    (handler/execute-action action-type (vec text-section) (vec data-section))))
