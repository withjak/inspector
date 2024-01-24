(ns inspector.test.core-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]))

(deftest run-before-rules|share-working?-test
  (let [c-t (fn [& _] true)
        c-f (fn [& _] false)
        a1 (fn [_meta-data _fn-args shared] shared)
        a2 (fn [_meta-data _fn-args shared] (assoc shared :a2 2))
        a3 (fn [_meta-data _fn-args shared] (assoc shared :a3 3))
        a4 (fn [_meta-data _fn-args shared] (assoc shared :a4 4))
        a5 (fn [_meta-data _fn-args shared] shared)
        rules [c-t a1 c-t a2 c-f a3 c-t a4 c-t a5]
        meta-data {}
        fn-args []]
    (is (= (core/run-before-rules rules meta-data fn-args {})
           {:a2 2, :a4 4}))))

(deftest run-after-rules|share-working?-test
  (let [c-t (fn [& _] true)
        c-f (fn [& _] false)
        a1 (fn [_meta-data _fn-args shared _return-value] shared)
        a2 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a2 2))
        a3 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a3 3))
        a4 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a4 4))
        a5 (fn [_meta-data _fn-args shared _return-value] shared)
        rules [c-t a1 c-t a2 c-f a3 c-t a4 c-t a5]
        meta-data {}
        fn-args []]
    (is (= (core/run-after-rules rules meta-data fn-args {} 1)
           {:a2 2, :a4 4}))))