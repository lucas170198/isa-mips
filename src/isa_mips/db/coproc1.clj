(ns isa-mips.db.coproc1
  (:require [schema.core :as s]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.models.instruction :as m.instruction]))

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

(s/defn set-hi!
  [hi-value :- s/Str]
  (reset! hi hi-value))

(s/defn set-lo!
  [lo-value :- s/Str]
  (reset! lo lo-value))

;TODO: Refactor, dont repeat code
(defn ^:private get-by-addr
  [address]
  (-> #(= (:addr %) address)
      (filterv @mem)
      first))

(defn ^:private get-by-name
  [name]
  (-> #(= (get-in % [:meta :name]) name)
      (filterv @mem)
      first))

(defn ^:private update-if
  "Update a element that matches with the pred"
  [coll value keys pred-fn]
  (mapv #(if (pred-fn %)
           (assoc-in % keys value) %) coll))

(s/defn read-value-by-name! :- m.instruction/fourBytesString
  [name :- s/Str]
  (get-in (get-by-name name) [:meta :value]))

(s/defn read-name!
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :name]))

(s/defn write-value! :- (s/maybe m.memory/Store)
  [address :- s/Int
   value :- m.instruction/fourBytesString]
  (if (nil? (get-by-addr address))
    (swap! mem conj {:addr address :meta {:value value}})
    (reset! mem (update-if @mem value [:meta :value] #(= (:addr %) address)))))

(s/defn read-value! :- m.instruction/fourBytesString
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :value]))