(ns inspector.stream
  (:require [inspector.core :as core]
            [inspector.utils :as utils]))

(defn action
  [export-fn meta-data fn-args shared]
  (let [record (utils/prepare-fn-record meta-data fn-args shared)]
    (export-fn record))
  shared)

(defn stream-template
  [export-fn]
  (core/create-template []
                        [utils/always (partial action export-fn)]))

(defn start-streaming
  [vars export-fn]
  (reset! core/modify-all true)
  (core/attach-template-permanent vars (stream-template export-fn)))
