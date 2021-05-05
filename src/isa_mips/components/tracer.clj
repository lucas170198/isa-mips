(ns isa-mips.components.tracer
  (:require [isa-mips.protocols.logger :as p-logger]
            [clojure.java.io :as io]
            [isa-mips.adapters.number-base :as a.number-base]
            [isa-mips.logic.binary :as l.binary]))

(defn ^:private log!
  [address index read-mode file-path]
  (let [bin-address (a.number-base/binary-string-signal-extend address 32)]
    (with-open [writer (io/writer file-path :append true)]
      (.write writer (format "%s %s (line# %s)\n" read-mode (l.binary/bin->hex-str bin-address) (l.binary/bin->hex-str index)))
      (.flush writer))))

(defrecord Tracer [file-path]
  p-logger/Logger
  (log-read! [_this address index instruction?]
    (log! address index (if instruction? "I" "R") file-path))

  (log-write! [_this address index]
    (log! address index "W" file-path)))

(defn new-tracer [file-path]
  (->Tracer file-path))