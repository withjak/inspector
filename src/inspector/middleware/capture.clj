(ns inspector.middleware.capture
  (:require [inspector.utils :as utils]))

(def store (atom []))

(defn capture-middleware
  [store handler]
  (fn [state]
    (let [new-state (handler state)
          record (utils/prepare-fn-record new-state)]
      (swap! store conj record)
      new-state)))
