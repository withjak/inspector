(ns inspector.capture
  (:require [inspector.core :as core]
            [inspector.utils :as utils]))

(def accumulator (atom []))

(defn action
  [meta-data fn-args shared]
  (let [record (utils/prepare-fn-record meta-data fn-args shared)]
    (swap! accumulator conj record))
  shared)

(def capture-template
  (core/create-template [utils/always action] [utils/always action]))

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

