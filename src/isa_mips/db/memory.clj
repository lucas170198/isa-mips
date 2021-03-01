(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.helpers :as helpers]))

(def pc-init 0x00400000)

(s/def ^:private pointers
  (list {:addr 28 :meta {:name "$gp" :value (helpers/binary-string 0x10008000)}}
        {:addr 29 :meta {:name "$sp" :value (helpers/binary-string 0x7fffeffc)}}
        {:addr 30 :meta {:name "$fp" :value (helpers/binary-string 0)}}
        {:addr 31 :meta {:name "$ra" :value (helpers/binary-string 0)}}))

(s/def  pc (atom pc-init))

(def mem
  (->> (concat (list {:addr 0 :meta {:name "$zero" :value (helpers/binary-string 0)}}
                     {:addr 1 :meta {:name "$at" :value (helpers/binary-string 0)}})
               (helpers/register-class "$v" 2 (range 2))
               (helpers/register-class "$a" 4 (range 4))
               (helpers/register-class "$t" 8 (range 8))
               (helpers/register-class "$s" 16 (range 8))
               (helpers/register-class "$t" 24 (range 8 9))
               (helpers/register-class "$k" 26 (range 2))
               pointers)
       vec
       (s/validate m.memory/Store)
       (atom)))

(defn ^:private get-by-addr
  [address]
  (-> #(= (:addr %) address)
      (filter @mem)
      first))

(defn ^:private get-by-name
  [name]
  (-> #(= (get-in % [:meta :name]) name)
      (filter @mem)
      first))

(defn ^:private update-if
  "Update a element that matches with the pred"
  [coll value keys pred-fn]
  (map #(if (pred-fn %)
          (assoc-in % keys value) %) coll))

(s/defn read-value! :- s/Str
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :value]))

(s/defn read-name! :- s/Str
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :name]))

(s/defn write-value! :- m.memory/Store
  [address :- s/Int
   value :- s/Str]
  (if (nil? (get-by-addr address))
    (swap! mem conj {:addr address :meta {:value value}})
    (reset! mem (update-if @mem value [:meta :value] #(= (:addr %) address)))))

(s/defn read-value-by-name! :- s/Str
  [name :- s/Str]
  (get-in (get-by-name name) [:meta :value]))

(s/defn inc-program-counter
  []
  (swap! pc inc))
