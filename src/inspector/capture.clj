(ns inspector.capture
  (:require [inspector.core :as core]
            [inspector.utils :as utils]))

(def accumulator (atom []))

(defn prepare-fn-record
  [meta-data fn-args {:keys [caller-thread-id t-id c-id id]}]
  {:fn-name          (utils/full-name meta-data)
   :fn-args          fn-args
   :id               id
   :c-id             c-id
   :t-id             t-id
   :caller-thread-id caller-thread-id})

(defn b-action
  [meta-data fn-args shared]
  (swap! accumulator conj (prepare-fn-record meta-data fn-args shared))
  shared)

(defn a-action
  [meta-data fn-args shared return-value]
  (swap!
    accumulator
    conj
    (assoc (prepare-fn-record meta-data fn-args shared) :fn-rv return-value))
  shared)

(def capture-template
  (core/create-template [utils/always b-action] [utils/always a-action]))

(defn run
  [vars f]
  (binding [core/*modify-fns* true]
    (let [executor (core/attach-template vars capture-template)
          rv (executor f)
          fn-call-records @accumulator]
      (reset! accumulator [])
      {:rv rv :fn-call-records fn-call-records})))
