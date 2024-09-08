(ns inspector.middleware.capture
  (:require [inspector.utils :as utils]))

(def accumulator (atom []))

(defn capture-middleware
  [handler]
  (fn [state]
    (let [new-state (handler state)
          record (utils/prepare-fn-record new-state)]
      (swap! accumulator conj record)
      new-state)))
