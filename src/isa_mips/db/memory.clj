(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.adapters.number-base :as a.number-base]))

(def pc-init 0x00400000)

(s/defn ^:private register-class
  [prefix init-idx vector]
  (map-indexed (fn [idx _]
                 {:addr (+ idx init-idx)
                  :meta {:name  (str prefix idx)
                         :value (a.number-base/binary-string-zero-extend 0)}}) vector))

(s/def ^:private pointers
  (list {:addr 28 :meta {:name "$gp" :value (a.number-base/binary-string-zero-extend 0x10008000 32)}}
        {:addr 29 :meta {:name "$sp" :value (a.number-base/binary-string-zero-extend 0x7fffeffc 32)}}
        {:addr 30 :meta {:name "$fp" :value (a.number-base/binary-string-zero-extend 0 32)}}
        {:addr 31 :meta {:name "$ra" :value (a.number-base/binary-string-zero-extend 0 32)}}))

(s/def  pc (atom pc-init))

(def mem
  (->> (concat (list {:addr 0 :meta {:name "$zero" :value (a.number-base/binary-string-zero-extend 0 32)}}
                     {:addr 1 :meta {:name "$at" :value (a.number-base/binary-string-zero-extend 0 32)}})
               (register-class "$v" 2 (range 2))
               (register-class "$a" 4 (range 4))
               (register-class "$t" 8 (range 8))
               (register-class "$s" 16 (range 8))
               (register-class "$t" 24 (range 8 9))
               (register-class "$k" 26 (range 2))
               pointers)
       vec
       (s/validate m.memory/Store)
       (atom)))

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
;TODO: Assert that this have 32bits
(s/defn read-value! :- s/Str
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :value]))

(s/defn read-name! :- s/Str
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :name]))

(s/defn write-value! :- (s/maybe m.memory/Store)
  [address :- s/Int
   value :- s/Str]
  (when-not (= address 0)
    (if (nil? (get-by-addr address))
      (swap! mem conj {:addr address :meta {:value value}})
      (reset! mem (update-if @mem value [:meta :value] #(= (:addr %) address))))))

(s/defn read-value-by-name! :- s/Str
  [name :- s/Str]
  (get-in (get-by-name name) [:meta :value]))

(s/defn inc-program-counter
  []
  (swap! pc + 4))

(s/defn sum-program-counter
  [value :- s/Int]
  #_(println "SUM PC: " (Integer/toHexString (+ pc value)))
  (swap! pc + value))

(s/defn set-program-counter
  [value :- s/Int]
  (reset! pc value))
