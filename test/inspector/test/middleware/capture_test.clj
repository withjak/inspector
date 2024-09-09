(ns inspector.test.middleware.capture-test
  (:require [clojure.test :refer :all]
            [inspector.middleware.capture :as capture]))

(deftest capture-middleware-test
  (let [store (atom [])
        new-handler (capture/capture-middleware store identity)
        state {:fn-meta {:name "foo" :ns "dummy"}
               :fn-args '(1 2)
               :id      29
               :tid     10
               :c-id    9
               :c-tid   10
               :c-chain [6 7 8 9]
               :uuid    (random-uuid)
               :time    10000
               :fn-rv   3}
        result (new-handler state)]
    (is (fn? new-handler))
    (is (= state result))
    (is (= (-> state
               (assoc :fn-name "dummy/foo")
               (dissoc :fn-meta)
               vector)
           @store))))
