(ns inspector.capture
  (:require [inspector.core :as core]
            [inspector.utils :as utils]))

(def accumulator (atom []))

(defn b-action
  [meta-data fn-args shared]
  (swap! accumulator conj (utils/prepare-fn-record meta-data fn-args shared))
  shared)

(defn a-action
  [meta-data fn-args shared return-value]
  (let [record (-> (utils/prepare-fn-record meta-data fn-args shared)
                   (assoc :fn-rv return-value))]
    (swap! accumulator conj record))
  shared)

(def capture-template
  (core/create-template [utils/always b-action] [utils/always a-action]))

(defn run
  [vars f]
  (binding [core/*modify-fns* true]
    (let [executor (core/attach-template vars capture-template)
          {:keys [rv e]} (try
                           {:rv (executor f)}
                           (catch Exception e
                             {:e e}))
          fn-call-records @accumulator]
      (reset! accumulator [])
      {:rv rv :e e :fn-call-records fn-call-records})))

