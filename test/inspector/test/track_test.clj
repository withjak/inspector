(ns inspector.test.track-test
  (:require [clojure.test :refer :all]
            [inspector.track :as track]))

(defn foo [x] x)
(defn boo [x] (foo (inc x)))

(defn middleware
  [store handler]
  (fn [state]
    (let [new-state (handler state)]
      (swap! store conj new-state)
      new-state)))

(deftest with-track-test
  (let [store (atom [])
        middlewares [{:name       :m
                      :middleware (partial middleware store)
                      :store store}]
        tracked-vars #{#'foo #'boo}
        {:keys [rv e records]} (track/with-track
                                 middlewares
                                 tracked-vars
                                 #(boo 2))]
    (is (= 3 rv))
    (is (= nil e))
    (is (contains? records :m))
    (is (= 2 (count (:m records))))
    (is (= [] @store))))
