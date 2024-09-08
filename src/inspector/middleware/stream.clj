(ns inspector.middleware.stream
  (:require [inspector.utils :as utils]))

(defn stream-middleware
  [export-fn handler]
  (fn [state]
    (let [new-state (handler state)
          record (utils/prepare-fn-record new-state)]
      (export-fn record)
      new-state)))
