(ns isa-mips.db.memory
  (:require [schema.core :as s]
            [clojure.spec.alpha :as s-a]
            [isa-mips.models.memory :as m.memory]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.models.instruction :as m.instruction]
            [clojure.string :as str]))

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

(s/def pc (atom pc-init))

(s/def jump-addr "Store the jump address when a instructions is a jump one. This is for solve the branch delay problem"
  (atom nil))


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

(s/defn read-reg-value! :- m.instruction/fourBytesString
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :value]))

(s/defn word-read-value!
  [initial-address :- s/Int]
  (-> #(s-a/int-in-range? initial-address (+ initial-address 4) (:addr %))
      (filterv @mem)
      (->> (map #(get-in % [:meta :value])))
      reverse
      str/join))

(s/defn read-name!
  [address :- s/Int]
  (get-in (get-by-addr address) [:meta :name]))

(s/defn write-value! :- (s/maybe m.memory/Store)
  [address :- s/Int
   value :- m.instruction/fourBytesString]
  (when-not (= address 0)
    (if (nil? (get-by-addr address))
      (swap! mem conj {:addr address :meta {:value value}})
      (reset! mem (update-if @mem value [:meta :value] #(= (:addr %) address))))))

(s/defn read-value-by-name! :- m.instruction/fourBytesString
  [name :- s/Str]
  (get-in (get-by-name name) [:meta :value]))

(s/defn inc-program-counter!
  []
  (swap! pc + 4))

(s/defn set-program-counter!
  [value :- s/Int]
  (reset! pc value))

(s/defn sum-jump-addr!
  [value :- s/Int]
  (reset! jump-addr (+ @pc value)))

(s/defn set-jump-addr!
  [value :- s/Int]
  (reset! jump-addr value))

(s/defn clear-jump-addr!
  []
  (reset! jump-addr nil))
