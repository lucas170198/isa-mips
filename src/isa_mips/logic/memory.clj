(ns isa-mips.logic.memory
  (:require [schema.core :as s]
            [isa-mips.models.memory :as models.memory]))

(defn- number-of-lines
  [{:keys [size line-size assoc-param]}]
  (->> (* line-size assoc-param)
       (/ size)
       int))

(defn- log2
  [value]
  (int (/ (Math/log value) (Math/log 2))))

(s/defn decode-address :- models.memory/DecodedAddress
  [address-bit-string :- s/Str
   {:keys [line-size] :as config} :- models.memory/MemConfig]
  (let [block-section-end (+ 2 (log2 (/ line-size 4)))
        index-section-end (+ block-section-end (log2 (number-of-lines config)))]
    {:byte-offset  (subs address-bit-string 0 2)
     :block-offset (subs address-bit-string 2 block-section-end)
     :index        (subs address-bit-string block-section-end index-section-end)
     :tag          (subs address-bit-string index-section-end 32)}))

(s/defn search-by-tag :- models.memory/memory-skeleton
  [cache-block tag]
  (let [valid-lines (filterv #(get-in % [:meta :valid]) cache-block)
        tag-filter  (filterv #(= (get-in % [:meta :tag]) tag) valid-lines)
        _assertion  (assert (<= (count tag-filter) 1) "Doubled tag error")]
    (first tag-filter)))
