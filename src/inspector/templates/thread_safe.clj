(ns inspector.templates.thread-safe
  (:require [inspector.core :as core]
            [inspector.templates.common :as common]))

(def accumulator (atom []))

(defn b-action
  [meta-data fn-args shared]
  (swap! accumulator conj {:fn-name      (common/full-name meta-data)
                           :fn-args      fn-args
                           :shared-state shared})
  shared)

(defn a-action
  [meta-data fn-args shared return-value]
  (swap! accumulator conj {:fn-name      (common/full-name meta-data)
                           :fn-args      fn-args
                           :fn-rv        return-value
                           :shared-state shared})
  shared)

(def example-template
  (core/create-template [common/always b-action]
                        [common/always a-action]))

(defn capture
  [vars f]
  (binding [core/*modify-fns* true]
    (let [rv ((core/attach-template vars example-template) f)
          captured-data @accumulator]
      (reset! accumulator [])
      {:rv rv :captured-data captured-data})))

