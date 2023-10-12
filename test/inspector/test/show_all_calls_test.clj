(ns inspector.test.show-all-calls-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [inspector.explore :as explore]))

(defn bar
  [a]
  (println a))

(defn foo
  [a]
  (bar a)
  {:a a})

(deftest show-all-calls-test|bar
  (let [my-project-vars [#'bar]
        output (with-out-str
                 (explore/show-all-calls my-project-vars #(bar 1)))
        expected-output (str/join
                          "\n"
                          ["Г-- \u001B[41minspector.test.show-all-calls-test/bar\u001B[0m (1)"
                           "1"
                           "L__ nil"])]
    ;; (explore/show-all-calls my-project-vars #(bar 1))
    (is (= expected-output (str/trim-newline output)))))

(deftest show-all-calls-test|foo
  (let [my-project-vars [#'bar #'foo]
        output (with-out-str
                 (explore/show-all-calls my-project-vars #(foo 1)))
        expected-output (str/join
                          "\n"
                          ["Г-- \u001B[41minspector.test.show-all-calls-test/foo\u001B[0m (1)"
                           "|  Г-- \u001B[42minspector.test.show-all-calls-test/bar\u001B[0m (1)"
                           "1"
                           "|  L__ nil"
                           "L__ {:a 1}"])]
    ;; (explore/show-all-calls my-project-vars #(foo 1))
    (is (= expected-output (str/trim-newline output)))))

(comment
  (bar 1)
  (foo 1)
  (var? #'foo)

  (def my-project-vars [#'foo #'bar])
  (explore/show-all-calls my-project-vars #(bar 1))
  (explore/show-all-calls my-project-vars #(foo 1)))