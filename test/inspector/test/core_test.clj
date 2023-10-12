(ns inspector.test.core-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]
            [inspector.templates.common :as common]))

(deftest run-before-rules|share-working?-test
  (let [c (fn [& _] true)
        a1 (fn [_meta-data _fn-args shared] shared)
        a2 (fn [_meta-data _fn-args shared] (assoc shared :a2 2))
        a3 (fn [_meta-data _fn-args shared] (assoc shared :a3 3))
        a4 (fn [_meta-data _fn-args shared] (assoc shared :a4 4))
        a5 (fn [_meta-data _fn-args shared] shared)
        rules [c a1 c a2 c a3 c a4 c a5]
        meta-data {}
        fn-args []]
    (is (= (core/run-before-rules rules meta-data fn-args {})
           {:a2 2, :a3 3, :a4 4}))))

(deftest run-after-rules|share-working?-test
  (let [c (fn [& _] true)
        a1 (fn [_meta-data _fn-args shared _return-value] shared)
        a2 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a2 2))
        a3 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a3 3))
        a4 (fn [_meta-data _fn-args shared _return-value] (assoc shared :a4 4))
        a5 (fn [_meta-data _fn-args shared _return-value] shared)
        rules [c a1 c a2 c a3 c a4 c a5]
        meta-data {}
        fn-args []]
    (is (= (core/run-after-rules rules meta-data fn-args {} 1)
           {:a2 2, :a3 3, :a4 4}))))

(defn bar
  [a]
  {:bar a})

(defn foo
  [a]
  {:foo (bar a)})

(deftest modify-fns-test
  (let [fn-to-execute #(foo 1)
        my-project-vars [#'foo #'bar]
        expected-captured-value [{:arglists '([a]) :fn-args '(1) :name 'foo}
                                 {:arglists '([a]) :fn-args '(1) :name 'bar}
                                 {:arglists '([a]) :fn-args '(1) :name 'bar :return-value {:bar 1}}
                                 {:arglists '([a]) :fn-args '(1) :name 'foo :return-value {:foo {:bar 1}}}]

        capture-data (atom [])
        b-action (fn [{:keys [arglists name] :as meta-data} fn-args shared]
                   (swap!
                     capture-data
                     conj
                     {:arglists arglists :name name :fn-args fn-args}))
        f-action (fn [{:keys [arglists name] :as meta-data} fn-args shared return-value]
                   (swap!
                     capture-data
                     conj
                     {:arglists arglists :name name :fn-args fn-args :return-value return-value}))
        before-rules [common/always b-action]
        after-rules [common/always f-action]
        template (core/create-template before-rules after-rules)

        executer (core/attach-template my-project-vars template)
        rv (executer fn-to-execute)]

    (is (= rv {:foo {:bar 1}}))
    (is (= @capture-data expected-captured-value))))