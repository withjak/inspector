(ns inspector.test.inspector-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [inspector.inspector :as i]))

(def file "/tmp/inspector_test.log")

;; Fixture
(defn delete-file [f]
  (spit file "")
  (f)
  (io/delete-file file))

(use-fixtures :each delete-file)

(defn simplest [i] i)

(defn simple [i] (simplest i))

(defn parallel [_] (vec (pmap simple (range 2))))

(def parallel-call-hierarchy-output-1
  "Time: Tue Jan 23 16:28:30 IST 2024
Г-- inspector.test.inspector-test/parallel (1)
|  Г-- inspector.test.inspector-test/simple (0)
|  |  Г-- inspector.test.inspector-test/simplest (0)
|  |  L-- 0
|  L-- 0
|  Г-- inspector.test.inspector-test/simple (1)
|  |  Г-- inspector.test.inspector-test/simplest (1)
|  |  L-- 1
|  L-- 1
L-- [0 1]
")

(def parallel-call-hierarchy-output-2
  "Time: Tue Jan 23 16:28:30 IST 2024
Г-- inspector.test.inspector-test/parallel (1)
|  Г-- inspector.test.inspector-test/simple (1)
|  |  Г-- inspector.test.inspector-test/simplest (1)
|  |  L-- 1
|  L-- 1
|  Г-- inspector.test.inspector-test/simple (0)
|  |  Г-- inspector.test.inspector-test/simplest (0)
|  |  L-- 0
|  L-- 0
L-- [0 1]
")

(deftest print-captured-data-test
  (let [my-project-vars #{#'simplest #'simple #'parallel}
        output (with-out-str (i/iprint my-project-vars #(parallel 1)))]
    (is (or
          (= (rest (str/split-lines parallel-call-hierarchy-output-1))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-call-hierarchy-output-2))
             (rest (str/split-lines output)))))))

(deftest spit-captured-data-test
  (let [my-project-vars #{#'simplest #'simple #'parallel}
        rv (i/ispit file my-project-vars #(parallel 1))
        output (slurp file)]
    (is (or
          (= (rest (str/split-lines parallel-call-hierarchy-output-1))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-call-hierarchy-output-2))
             (rest (str/split-lines output)))))))

