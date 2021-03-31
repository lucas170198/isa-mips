(ns isa-mips.models.action
  (:require [schema.core :as s]))

(def Type (s/enum :decode :run))
