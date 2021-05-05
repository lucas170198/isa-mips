(ns isa-mips.protocols.logger
  (:require [schema.core :as s]))

(defprotocol Logger "Log interface"
  (log-read! [component address index instruction?])
  (log-write! [component address index]))

(def ILogger (s/protocol Logger))
