(ns isa-mips.helpers)

(defn register-class
  [prefix init-idx vector]
  (into {} (map-indexed (fn [idx _]
                          {(+ idx init-idx) {:name  (str prefix idx)
                                             :value 0}}) vector)))
