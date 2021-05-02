(ns isa-mips.controllers.simulation-summary
  (:require [schema.core :as s]
            [isa-mips.db.simulation-summary :as db.simulation-summary]
            [isa-mips.logic.stats :as l.stats]))

(def speedup-times 4.00)
(def stall-pipeline-cycles 4)
;TODO: Will be a function when implements the memory logic
(def ipc 1)
(def base-freq 8467200)

(s/defn ^:private print-exec-summary!
  []
  (let [execution-time-sec (l.stats/millis->sec (db.simulation-summary/millis-since-started))
        {:keys [R I J FR FI
                average-ips
                total-inst]} (l.stats/exec-summary @db.simulation-summary/instructions-summary execution-time-sec)]
    (println (format "Instruction count: %d (R: %d I: %d J: %d FR: %d FI: %d)" total-inst R I J FR FI))
    (println (format "Simulation time: %.2f sec" (float execution-time-sec)))
    (println (format "Average IPS: %.2f" (float average-ips)))))

(s/defn ^:private print-time-stats
  [{:keys [cycles frequency exec-time ipc mips]}]
  (println (format "%5s Cycles: %d", "", cycles))
  (println  (format "%5s Frequency: %.4f MHz", "", (float (/ frequency 1000000))))
  (println (format "%5s Estimated execution time: %.4f sec", "", (float exec-time)))
  (println (format "%5s IPC: %.2f", "", (float ipc)))
  (println (format "%5s MIPS: %.2f", "", (float mips))))

(s/defn ^:private print-times-summary!
  []
  (let [instructions-total (l.stats/instructions-total @db.simulation-summary/instructions-summary)
        monocycle (l.stats/execution-time-summary ipc  base-freq instructions-total 0)
        pipelined (l.stats/execution-time-summary ipc (* base-freq speedup-times) instructions-total stall-pipeline-cycles)]
    (println "Monocycle")
    (print-time-stats monocycle)
    (println "Pipelined")
    (print-time-stats pipelined)
    (println (str "Speedup Monocycle/Pipelined: " speedup-times "X"))))

(s/defn print-stats!
  []
  (println "Execution finished successfully")
  (println "--------------------------------")
  (print-exec-summary!)
  (println)
  (println "Simulated execution times for:")
  (println "-------------------------------")
  (print-times-summary!))


