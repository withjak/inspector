(ns inspector.test.export-test
  (:require [clojure.test :refer :all]
            [inspector.explore :as explore]))


(defn bar
  [a]
  (println a))

(defn foo
  [a]
  (bar a)
  {:a a})

(deftest export|foo
  (let [my-project-vars [#'foo #'bar]
        captured-data (atom [])
        exporter-fn (fn [_ data] (swap! captured-data conj data))
        _fn-call-rv (explore/export
                      exporter-fn
                      nil
                      my-project-vars
                      #(foo 1))
        expected-output [{:fn_args    '(1)
                          ;; :fn_call_id 100
                          :fn_name    "inspector.test.export-test/foo"
                          :level      0
                          :t_id       20
                          ;; :t_name          "nREPL-session-4bbc0bb7-a885-451d-8e52-6b49b8bae0c7"
                          }
                         {:fn_args    '(1)
                          ;; :fn_call_id 101
                          :fn_name    "inspector.test.export-test/bar"
                          :level      1
                          :t_id       20
                          ;; :t_name     "nREPL-session-4bbc0bb7-a885-451d-8e52-6b49b8bae0c7"
                          }
                         {:fn_args         '(1)
                          ;; :fn_call_id      101
                          :fn_name         "inspector.test.export-test/bar"
                          :fn_return_value nil
                          :level           1
                          :t_id            20
                          ;; :t_name          "nREPL-session-4bbc0bb7-a885-451d-8e52-6b49b8bae0c7"
                          }
                         {:fn_args         '(1)
                          ;; :fn_call_id      100
                          :fn_name         "inspector.test.export-test/foo"
                          :fn_return_value {:a 1}
                          :level           0
                          :t_id            20
                          ;; :t_name          "nREPL-session-4bbc0bb7-a885-451d-8e52-6b49b8bae0c7"
                          }]]
    (is (= expected-output (vec (map #(dissoc % :t_name :fn_call_id) @captured-data))))
    (let [fn-call-ids (map :fn_call_id @captured-data)]
      (testing "fn-call-id value"
        (testing "fn-call-id's are integers"
          (is (= (count fn-call-ids) (count (filter int? fn-call-ids)))))
        (testing "fn-call-ids make a palindrome"
          (is (= fn-call-ids (reverse fn-call-ids))))))))

(comment
  (bar 1)
  (foo 1)

  (def my-project-vars [#'foo #'bar])

  ;; default
  (explore/export "/tmp/data.json" my-project-vars #(bar 1))

  (explore/export "/tmp/data.json" my-project-vars #(foo 1))

  (explore/export
    (fn [_ data] (println data))
    nil
    my-project-vars
    #(foo 1))

  (explore/export
    (fn [_ data] (clojure.pprint/pprint data))
    nil
    my-project-vars
    #(foo 1)))