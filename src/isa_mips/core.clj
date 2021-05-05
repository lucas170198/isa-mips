(ns isa-mips.core
  (:gen-class)
  (:require [isa-mips.adapters.file-input :as a.file-input]
            [isa-mips.handler :as handler]
            [isa-mips.components.registers-storage :as registers-storage]
            [isa-mips.components.memory-storage :as memory-storage]))

(defn -main
  [& args]
  (let [action-type    (a.file-input/action->action-type (first args))
        memory         (memory-storage/new-memory (if (= (count args) 3) (Integer/parseInt (second args)) 1))
        storage        (registers-storage/new-register-storage!)
        coproc-storage (registers-storage/new-coproc1-storage!)
        text-section   (a.file-input/file->bytes (last args) "text")
        data-section   (a.file-input/file->bytes (last args) "data")
        rodata-section (a.file-input/file->bytes (last args) "rodata")]
    (handler/execute-action action-type
                            (vec text-section)
                            (vec data-section)
                            (vec rodata-section)
                            storage coproc-storage memory)))
