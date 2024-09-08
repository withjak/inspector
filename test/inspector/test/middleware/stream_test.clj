(ns inspector.test.middleware.stream-test
  (:require [clojure.test :refer :all]
            [inspector.middleware.stream :as stream]))

(deftest stream-middleware-test
  (let [store (atom [])
        capture (fn [record]
                  (swap! store conj record))
        new-handler (stream/stream-middleware capture identity)
        state {:meta-data {:name "foo" :ns "dummy"}
               :fn-args   '(1 2)
               :id        29
               :tid       10
               :c-id      9
               :c-tid     10
               :c-chain   [6 7 8 9]
               :uuid      (random-uuid)
               :time      10000
               :fn-rv     3}
        result (new-handler state)]
    (is (fn? new-handler))
    (is (= state result))
    (is (= (-> state
               (assoc :fn-name "dummy/foo")
               (dissoc :meta-data)
               vector)
           @store))))