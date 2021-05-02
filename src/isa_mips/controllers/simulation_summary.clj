(ns isa-mips.controllers.simulation-summary
  (:require [schema.core :as s]
            [isa-mips.db.simulation-summary :as db.simulation-summary]))

;TODO: Move logics to another namespace

(defn- instructions-total
  [inst-count]
  (reduce + 0 (vals inst-count)))

(s/defn ^:private print-exec-summary!
  []
  (let [execution-time-sec (/ (db.simulation-summary/millis-since-started) 1000)
        {:keys [R I J FR FI] :as instructions-count} @db.simulation-summary/instructions-count
        instruction-total  (instructions-total instructions-count)
        average-ips        (/ instruction-total execution-time-sec)]
    (println (str "Instruction count: " instruction-total " ( R:" R ", I:" I ", J:" J ", FR:" FR ", FI:" FI ")"))
    (println (str "Simulation Time: " (float execution-time-sec) " s"))
    (println (str "Average IPS: " (float average-ips)))))

(s/defn ^:private execution-time-summary
  [ipc freq instruction-total pipeline-stall]
  (let [cycles (+ (* instruction-total ipc) pipeline-stall)]
    {:cycles    cycles
     :frequency freq
     :exec-time (float (* cycles (/ 1 freq)))
     :ipc       (float ipc)
     :mips      (float (/ (* ipc freq) 1000000))}))

(s/defn ^:private print-pipeline-summary!
  []
  (let [speedup-times 4.00
        base-freq 8467200
        instructions-total (instructions-total @db.simulation-summary/instructions-count)
        monocycle (execution-time-summary 1  base-freq instructions-total 0)
        pipelined (execution-time-summary 1 (* base-freq speedup-times) instructions-total 4)]
    (println "Simulated execution times for:")
    (println "-------------------------------")
    (println "Monocycle")
    (clojure.pprint/print-table [monocycle])
    (println "Pipelined")
    (clojure.pprint/print-table [pipelined])
    (println (str "Speedup Monocycle/Pipelined: " speedup-times "X"))))

(s/defn print-stats!
  []
  (println "Execution finished successfully")
  (println "--------------------------------")
  (print-exec-summary!)
  (println)
  (print-pipeline-summary!))


