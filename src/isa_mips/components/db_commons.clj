(ns isa-mips.components.db-commons)

(defn ^:private assoc-if
  "Update a element that matches with the pred"
  [coll value keys pred-fn]
  (mapv #(if (pred-fn %)
           (assoc-in % keys value) %) coll))

(defn ^:private get-by-address
  [storage address]
  (-> #(= (:addr %) address)
      (filterv storage)))

(defn get-db
  [storage]
  @storage)

(defn get-from-storage
  [storage address]
  (get-by-address (get-db storage) address))

(defn  first-by-address
  [storage address]
  (first (get-by-address storage address)))

(defn ^:private first-from-storage
  [storage address]
  (first-by-address (get-db storage) address))

(defn read-value-from-storage
  [storage address]
  (-> (first-from-storage storage address)
      (get-in [:meta :value])))

(defn write-to-mem!
  [storage address byte]
  (if (nil? (first-by-address @storage address))
    (swap! storage conj {:addr address :meta {:value byte}})
    (reset! storage (assoc-if (get-db storage) byte [:meta :value] #(= (:addr %) address)))))
