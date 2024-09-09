(ns inspector.test.track-test
  (:require [clojure.test :refer :all]
            [inspector.track :as track]))

(defn foo [x] x)
(defn boo [x] (foo (inc x)))

(defn middleware
  [store handler]
  (fn [state]
    (let [new-state (handler state)]
      (swap! store conj (assoc new-state :m-name :test-middleware))
      new-state)))

(deftest with-track-test
  (let [store (atom [])
        middlewares [(partial middleware store)]
        tracked-vars #{#'foo #'boo}
        {:keys [rv e records]} (track/with-track
                                 middlewares store
                                 tracked-vars
                                 #(boo 2))]
    (is (= 3 rv))
    (is (= nil e))
    (is (= [:test-middleware :test-middleware] (map :m-name records)))
    (is (= 2 (count records)))))
