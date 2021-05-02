(ns isa-mips.logic.stats)

;TODO: Create a model for the instruction counter
(defn instructions-total
  [inst-count]
  (reduce + 0 (vals inst-count)))

(defn millis->sec
  [millis]
  (/ millis 1000))

(defn execution-time-summary
  [ipc freq instruction-total pipeline-stall]
  (let [cycles (+ (* instruction-total ipc) pipeline-stall)]
    {:cycles    cycles
     :frequency freq
     :exec-time (* cycles (/ 1 freq))
     :ipc       ipc
     :mips      (/ (* ipc freq) 1000000)}))

(defn exec-summary
  [instructions-summary execution-time-sec]
  (let [total (instructions-total instructions-summary)]
    (assoc instructions-summary
      :total-inst total
      :average-ips (/ total execution-time-sec))))


