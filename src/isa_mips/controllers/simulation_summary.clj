(ns isa-mips.controllers.simulation-summary
  (:require [schema.core :as s]
            [isa-mips.db.simulation-summary :as db.simulation-summary]))

(s/defn ^:provate exec-summary
  []
  (let [execution-time-sec (/ (db.simulation-summary/millis-since-started) 1000)
        instruction-total (reduce + 0 (vals @db.simulation-summary/instructions-count))]
    (merge @db.simulation-summary/instructions-count
           {:total          instruction-total
            :execution-time execution-time-sec
            :average-ips    (/ instruction-total execution-time-sec)})))

(s/defn print-stats!
  []
  (let [exec-info (exec-summary)]
    (clojure.pprint/print-table (keys exec-info) [exec-info])))


