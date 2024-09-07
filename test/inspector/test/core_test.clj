(ns inspector.test.core-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]
            [inspector.utils :as utils]))

(deftest run-rules|share-working?-test
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
    (is (= (core/run-rules rules meta-data fn-args {})
           {:a2 2, :a4 4}))))

(defn foo [a] a)

(deftest attach-template-permanent-meta-data-modification-test
  (let [template (core/create-template [] [])
        fn-vars #{#'foo}
        old-meta-data (meta #'foo)]
    (core/attach-template-permanent fn-vars template)
    (let [new-meta-data (meta #'foo)]
      (is (contains? new-meta-data :i-original-value)))))

(deftest attach-template-permanent-fn-value-test
  (let [DATA (atom [])
        store-data (fn [_meta-data _fn-args shared]
                     (swap! DATA conj shared))
        template (core/create-template [] [utils/always store-data])]
    (core/attach-template-permanent #{#'foo} template)
    (foo :a)
    ; modified that's why we were able to capture some data, like :time
    (is (contains? (first @DATA) :time))))

(deftest restore-original-value-test
  (let [DATA (atom [])
        store-data (fn [_meta-data _fn-args shared]
                     (swap! DATA conj shared))
        template (core/create-template [] [utils/always store-data])
        fn-vars #{#'foo}]
    (core/attach-template-permanent fn-vars template)
    (foo :a)
    (is (contains? (meta #'foo) :i-original-value))
    (is (not= [] @DATA))

    (reset! DATA [])
    (core/restore-original-value fn-vars)
    (is (= false (contains? (meta #'foo) :i-original-value)))
    (is (= [] @DATA))))

