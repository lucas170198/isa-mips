(ns isa-mips.adapters.file-input
  (:require [schema.core :as s]
            [isa-mips.models.action :as m.action]))

(s/defn action->action-type :- m.action/Type
  [action :- s/Str]
  (s/validate m.action/Type (keyword action)))
