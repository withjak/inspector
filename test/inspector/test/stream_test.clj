(ns inspector.test.stream-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]
            [inspector.stream :as stream]))

(defn simplest [i] i)
(defn simple [i] (simplest i))
(defn nested [_] (simple 1))

(defn simplest-fail [i] (/ i 0))
(defn simple-fail [i] (simplest-fail i))
(defn nested-fail [_] (simple-fail 1))

(def STREAMED-DATA (atom []))

(defn store-data
  [data]
  (swap! STREAMED-DATA conj data))

(defn setup-test [test]
  (let [my-project-vars [#'simplest #'simple #'nested
                         #'simplest-fail #'simple-fail #'nested-fail
                         ]]
    (stream/start-streaming my-project-vars store-data)
    (test)
    (core/restore-original-value my-project-vars)
    (reset! STREAMED-DATA [])))

(use-fixtures :each setup-test)

(deftest stream-from-single-thread-test
  (nested 1)
  (is (= 6 (count @STREAMED-DATA)))

  (let [freq (-> (map :t-id @STREAMED-DATA)
                 frequencies)]
    (is (= 1 (count freq)))
    (is (= 6 (first (vals freq)))))

  (let [freq (-> (map :fn-name @STREAMED-DATA)
                 frequencies)]
    (is (= {"inspector.test.stream-test/nested"   2
            "inspector.test.stream-test/simple"   2
            "inspector.test.stream-test/simplest" 2} freq))))

(deftest stream-from-single-thread-exception-test
  (is (thrown? ArithmeticException
               (nested-fail 1)))
  (is (= 6 (count @STREAMED-DATA)))

  (let [freq (-> (map :t-id @STREAMED-DATA)
                 frequencies)]
    (is (= 1 (count freq)))
    (is (= 6 (first (vals freq)))))

  (is (= {"inspector.test.stream-test/nested-fail"   2
          "inspector.test.stream-test/simple-fail"   2
          "inspector.test.stream-test/simplest-fail" 2}
         (-> (map :fn-name @STREAMED-DATA)
             frequencies)))

  (is (= 3
         (->> (filter :e @STREAMED-DATA)
              (map :e)
              count)))
  (is (= 1
         (->> (filter :e @STREAMED-DATA)
              (map :e)
              set
              count)))
  (is (= '(:via :trace :cause)
         (->> (filter :e @STREAMED-DATA)
              (map :e)
              set
              first
              keys))))

; TODO: expand on this test
(deftest stream-from-multiple-independent-threads-test
  (future (nested 1))
  (future (nested 2))
  (Thread/sleep 2000)

  (is (= 12 (count @STREAMED-DATA)))

  (let [freq (-> (map :id @STREAMED-DATA)
                 frequencies)]
    (is (= [2 2 2 2 2 2] (vals freq)))))

; TODO: expand on this test
(deftest stream-from-multiple-independent-threads-exceptions-test
  ; future do not raises its exception to caller thread. Until asked explicitly
  (future (nested-fail 1))
  (future (nested-fail 2))
  (Thread/sleep 2000)
  (is (= 12 (count @STREAMED-DATA))))

(comment
  (-> (get (ns-interns *ns*) 'skip-fixture-test)
      meta))


