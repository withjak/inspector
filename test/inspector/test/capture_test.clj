(ns inspector.test.capture-test
  (:require [clojure.test :refer :all]
            [inspector.capture :as capture]))

(defn simplest [i] i)

(defn simple [i] (simplest i))

(defn parallel [_] (vec (pmap simple (range 2))))

; TODO: some times there are 4 threads ans sometimes 3. Check why is that happening.
(deftest parallel-test
  (let [my-project-vars [#'simplest #'simple #'parallel]
        {:keys [e rv fn-call-records]} (capture/run my-project-vars #(parallel 1))
        groups (group-by :fn-name fn-call-records)
        rv-records (filter :fn-rv fn-call-records)
        execution-time (map :execution-time rv-records)
        id-s (map :id fn-call-records)
        c-id-s (map :c-id fn-call-records)
        c-t-id-s (map :c-tid fn-call-records)
        t-id-s (map :tid fn-call-records)
        c-chains (map :c-chain fn-call-records)]

    (testing "count of records"
      (is (= 10 (count fn-call-records)))
      (is (= 5 (count rv-records)))
      ; records are same before and after running function
      (is (= (->> fn-call-records
                  (filter (comp not :fn-rv))
                  set)
             (->> rv-records
                  (map #(dissoc % :fn-rv :execution-time))
                  set)))
      ; only these keys should be present
      (is (= {[:fn-name :fn-args :id :tid :c-id :c-tid :c-chain :uuid]                        5
              [:tid :c-chain :fn-name :execution-time :id :fn-args :c-id :uuid :c-tid :fn-rv] 5}
             (->> fn-call-records
                  (map keys)
                  frequencies))))

    (testing "testing correct args, rv and function calls"
      (is (= (->> rv-records
                  (map #(select-keys % [:fn-name :fn-args :fn-rv]))
                  set)
             #{{:fn-args '(1)
                :fn-name "inspector.test.capture-test/simplest"
                :fn-rv   1}
               {:fn-args '(1)
                :fn-name "inspector.test.capture-test/simple"
                :fn-rv   1}
               {:fn-args '(0)
                :fn-name "inspector.test.capture-test/simplest"
                :fn-rv   0}
               {:fn-args '(0)
                :fn-name "inspector.test.capture-test/simple"
                :fn-rv   0}
               {:fn-args '(1)
                :fn-name "inspector.test.capture-test/parallel"
                :fn-rv   [0 1]}})))

    (is (every? int? id-s))
    (is (= {java.lang.Long 8 nil 2}
           (-> (group-by type c-id-s)
               (update-vals count))))
    (is (= {java.lang.Long 8
            nil            2}
           (-> (group-by type c-t-id-s)
               (update-vals count))))
    (is (every? int? t-id-s))
    (is (every? int? execution-time))

    (testing "total records captured"
      (is (= (count fn-call-records) 10))
      (let [g (group-by #(contains? % :fn-rv) fn-call-records)]
        (is (=
              (count (get g true))
              (count (get g false))
              (/ (count fn-call-records) 2)))))

    (testing "number of unique threads created"
      (is (= (->> fn-call-records
                  (map :tid)
                  set
                  count)
             3))
      ; includes nil as well
      (is (= (->> fn-call-records
                  (map :c-tid)
                  set
                  count)
             4)))

    (let [s (get groups "inspector.test.capture-test/parallel")
          args (map :fn-args s)
          rv (map :fn-rv s)]
      (is (= (count s) 2))
      (is (= (frequencies args) {'(1) 2}))
      (is (= (frequencies rv) {[0 1] 1 nil 1}))
      (doseq [{:keys [caller-thread-id t-id]} s]
        (is (= caller-thread-id t-id))))

    (let [s (get groups "inspector.test.capture-test/simple")
          args (map :fn-args s)
          rv (map :fn-rv s)]
      (is (= (count s) 4))
      (is (= (frequencies args) {'(0) 2 '(1) 2}))
      (is (= (frequencies rv) {0 1 1 1 nil 2}))
      (doseq [{:keys [c-tid t-id]} s]
        (is (not= c-tid t-id))))

    (let [s (get groups "inspector.test.capture-test/simplest")
          args (map :fn-args s)
          rv (map :fn-rv s)]
      (is (= (count s) 4))
      (is (= (frequencies args) {'(0) 2 '(1) 2}))
      (is (= (frequencies rv) {0 1 1 1 nil 2}))
      (doseq [{:keys [caller-thread-id t-id]} s]
        (is (= caller-thread-id t-id))))))


(defn simplest-fail [i] (/ i 0))

(defn simple-fail [i] (simplest-fail i))

(defn parallel-fail [_] (vec (pmap simple-fail (range 2))))

(deftest stack-trace-is-usable-test
  (let [my-project-vars [#'simplest-fail #'simple-fail #'parallel-fail]
        {:keys [e rv fn-call-records]} (capture/run my-project-vars #(parallel-fail 1))]
    (is (not= nil e))
    (is (let [names (->> (Throwable->map e)
                         :trace
                         (map first)
                         (map name)
                         set)]
          (every?
            #(contains? names %)
            ["inspector.test.capture_test$simple_fail"
             "inspector.test.capture_test$simplest_fail"])
          ; TODO: why is inspector.test.capture_test$parallel_fail not showing up?
          ))

    (let [groups (group-by :fn-name fn-call-records)
          rv-records (filter #(contains? % :fn-rv) fn-call-records)]
      (is (every? :e rv-records)))))
