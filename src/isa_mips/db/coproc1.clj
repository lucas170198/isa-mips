(ns isa-mips.db.coproc1
  (:require [schema.core :as s]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.models.memory :as m.memory]))

;TODO: Refactor, dont repeat code
(s/defn ^:private register-class
        [prefix init-idx vector]
        (map-indexed (fn [idx _]
                       {:addr (+ idx init-idx)
                        :meta {:name  (str prefix idx)
                               :value (a.number-base/binary-string-zero-extend 0)}}) vector))
(def mem
  (->> (vec (register-class "$f" 0 (range 32)))
       (s/validate m.memory/Store)
       (atom)))

(def hi (atom (a.number-base/binary-string-zero-extend 0)))

(def lo (atom (a.number-base/binary-string-zero-extend 0)))

;TODO: Refactor, dont repeat code
(defn ^:private get-by-addr
  [address]
  (-> #(= (:addr %) address)
      (filterv @mem)
      first))

(defn read-value-by-name! [param1]
  )

(s/defn read-name!
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :name]))