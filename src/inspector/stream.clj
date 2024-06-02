(ns inspector.stream
  (:require [inspector.core :as core]
            [inspector.utils :as utils]))

(defn b-action
  [export-fn meta-data fn-args shared]
  (export-fn (utils/prepare-fn-record meta-data fn-args shared))
  shared)

(defn a-action
  [export-fn meta-data fn-args shared return-value]
  (let [record (-> (utils/prepare-fn-record meta-data fn-args shared)
                   (assoc :fn-rv return-value))]
    (export-fn record))
  shared)

(defn stream-template
  [export-fn]
  (core/create-template [utils/always (partial b-action export-fn)]
                        [utils/always (partial a-action export-fn)]))

(defn start-streaming
  [vars export-fn]
  (reset! core/modify-all true)
  (core/attach-template-permanent vars (stream-template export-fn)))
