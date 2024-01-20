(ns inspector.test.example-test
  (:require [clojure.test :refer :all]
            [inspector.templates.thread-safe :as ts]))

(defn simplest [i] i)

(defn simple [i] (simplest i))

(defn parallel [_] (vec (pmap simple (range 2))))

(deftest simplest-test
  (let [my-project-vars [#'simplest]
        {:keys [rv captured-data]} (ts/capture my-project-vars #(simplest 1))]
    (is (= captured-data [{:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simplest"
                           :shared-state {:level            0
                                          :parent-thread-id 34
                                          :t-id             34}}
                          {:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simplest"
                           :fn-rv        1
                           :shared-state {:level            0
                                          :parent-thread-id 34
                                          :t-id             34}}]))))

(deftest simple-test
  (let [my-project-vars [#'simplest #'simple]
        {:keys [rv captured-data]} (ts/capture my-project-vars #(simple 1))]
    (is (= captured-data [{:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simple"
                           :shared-state {:level 0 :parent-thread-id 34 :t-id 34}}
                          {:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simplest"
                           :shared-state {:level 1 :parent-thread-id 34 :t-id 34}}
                          {:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simplest"
                           :fn-rv        1
                           :shared-state {:level 1 :parent-thread-id 34 :t-id 34}}
                          {:fn-args      '(1)
                           :fn-name      "inspector.test.example-test/simple"
                           :fn-rv        1
                           :shared-state {:level 0 :parent-thread-id 34 :t-id 34}}]))))

(deftest parallel-test
  (let [my-project-vars [#'simplest #'simple #'parallel]
        {:keys [rv captured-data]} (ts/capture my-project-vars #(parallel 1))
        groups (group-by :fn-name captured-data)]

    (is (= (count captured-data) 10))
    (is (= (->> captured-data
                (map :shared-state)
                (map :parent-thread-id)
                set
                count)
           3))
    (is (= (->> captured-data
                (map :shared-state)
                (map :t-id)
                set
                count)
           3))
    (let [g (group-by #(contains? % :fn-rv) captured-data)]
      (is (=
            (count (get g true))
            (count (get g false))
            (/ (count captured-data) 2))))

    (let [s (get groups "inspector.test.example-test/parallel")
          args (map :fn-args s)
          rv (map :fn-rv s)
          shared-states (map :shared-state s)
          levels (map :level shared-states)]
      (is (= (count s) 2))
      (is (= (frequencies args) {'(1) 2}))
      (is (= (frequencies rv) {[0 1] 1 nil 1}))
      (is (= (frequencies levels) {0 2}))
      (doseq [{:keys [parent-thread-id t-id]} shared-states]
        (is (= parent-thread-id t-id))))

    (let [s (get groups "inspector.test.example-test/simple")
          args (map :fn-args s)
          rv (map :fn-rv s)
          shared-states (map :shared-state s)
          levels (map :level shared-states)]
      (is (= (count s) 4))
      (is (= (frequencies args) {'(0) 2 '(1) 2}))
      (is (= (frequencies rv) {0 1 1 1 nil 2}))
      (is (= (frequencies levels) {1 4}))
      (doseq [{:keys [parent-thread-id t-id]} shared-states]
        (is (not= parent-thread-id t-id))))

    (let [s (get groups "inspector.test.example-test/simplest")
          args (map :fn-args s)
          rv (map :fn-rv s)
          shared-states (map :shared-state s)
          levels (map :level shared-states)]
      (is (= (count s) 4))
      (is (= (frequencies args) {'(0) 2 '(1) 2}))
      (is (= (frequencies rv) {0 1 1 1 nil 2}))
      (is (= (frequencies levels) {2 4}))
      (doseq [{:keys [parent-thread-id t-id]} shared-states]
        (is (= parent-thread-id t-id))))))
