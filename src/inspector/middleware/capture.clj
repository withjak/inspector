(ns inspector.middleware.capture
  (:require [inspector.utils :as utils]))

(defn capture-middleware
  [store handler]
  (fn [state]
    (let [new-state (handler state)
          record (utils/prepare-fn-record new-state)]
      ; store is supposed to be shared between all middlewares
      ; so when storing data in store, middleware must always associate its name in the data.
      (swap! store conj (assoc record :m-name :capture-middleware))
      new-state)))
