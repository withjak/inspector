(ns inspector.test.new-printer-test
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
Г-- inspector.test.new-printer-test/parallel (1)
|  Г-- inspector.test.new-printer-test/simple (0)
|  |  Г-- inspector.test.new-printer-test/simplest (0)
|  |  L-- 0
|  L-- 0
|  Г-- inspector.test.new-printer-test/simple (1)
|  |  Г-- inspector.test.new-printer-test/simplest (1)
|  |  L-- 1
|  L-- 1
L-- [0 1]
")

(def parallel-call-hierarchy-output-2
  "Time: Tue Jan 23 16:28:30 IST 2024
Г-- inspector.test.new-printer-test/parallel (1)
|  Г-- inspector.test.new-printer-test/simple (1)
|  |  Г-- inspector.test.new-printer-test/simplest (1)
|  |  L-- 1
|  L-- 1
|  Г-- inspector.test.new-printer-test/simple (0)
|  |  Г-- inspector.test.new-printer-test/simplest (0)
|  |  L-- 0
|  L-- 0
L-- [0 1]
")

(deftest print-captured-data-test
  (let [my-project-vars [#'simplest #'simple #'parallel]
        output (with-out-str (i/print-captured-data my-project-vars #(parallel 1)))]
    (is (or
          (= (rest (str/split-lines parallel-call-hierarchy-output-1))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-call-hierarchy-output-2))
             (rest (str/split-lines output)))))))

(deftest spit-captured-data-test
  (let [my-project-vars [#'simplest #'simple #'parallel]
        rv (i/spit-captured-data file my-project-vars #(parallel 1))
        output (slurp file)]
    (is (or
          (= (rest (str/split-lines parallel-call-hierarchy-output-1))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-call-hierarchy-output-2))
             (rest (str/split-lines output)))))))

; \u0020 space in unicode because intellij is removing the trailing space.
(def parallel-tracked-vars-output-1|simple
  "Time: Tue Jan 23 17:15:51 IST 2024
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\"]
name: inspector.test.new-printer-test/simple
args: (0)
rv: 0
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\"]
name: inspector.test.new-printer-test/simple
args: (1)
rv: 1
")

(def parallel-tracked-vars-output-2|simple
  "Time: Tue Jan 23 17:15:51 IST 2024
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\"]
name: inspector.test.new-printer-test/simple
args: (1)
rv: 1
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\"]
name: inspector.test.new-printer-test/simple
args: (0)
rv: 0
")

(deftest print-calls-to-tracked-vars-test
  (let [my-project-vars #{#'simplest #'simple #'parallel}
        output (with-out-str
                 (i/print-calls-to-tracked-vars #{#'simple #'simplest} my-project-vars #(parallel 1)))]
    (is (or
          (= (rest (str/split-lines parallel-tracked-vars-output-1|simple))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-tracked-vars-output-2|simple))
             (rest (str/split-lines output)))))))

(deftest spit-calls-to-tracked-vars-test|simple
  (let [my-project-vars #{#'simplest #'simple #'parallel}
        rv (i/spit-calls-to-tracked-vars file #{#'simple #'simplest} my-project-vars #(parallel 1))
        output (slurp file)]
    (is (or
          (= (rest (str/split-lines parallel-tracked-vars-output-1|simple))
             (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-tracked-vars-output-2|simple))
             (rest (str/split-lines output)))))))

(def parallel-tracked-vars-output-1|simplest
  "Time: Tue Jan 23 17:32:22 IST 2024
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\" \"inspector.test.new-printer-test/simplest\"]
name: inspector.test.new-printer-test/simplest
args: (1)
rv: 1
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\" \"inspector.test.new-printer-test/simplest\"]
name: inspector.test.new-printer-test/simplest
args: (0)
rv: 0
")

(def parallel-tracked-vars-output-2|simplest
  "Time: Tue Jan 23 17:32:22 IST 2024
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\" \"inspector.test.new-printer-test/simplest\"]
name: inspector.test.new-printer-test/simplest
args: (0)
rv: 0
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢\u0020
call-chain: [\"inspector.test.new-printer-test/parallel\" \"inspector.test.new-printer-test/simple\" \"inspector.test.new-printer-test/simplest\"]
name: inspector.test.new-printer-test/simplest
args: (1)
rv: 1
")

(deftest spit-calls-to-tracked-vars-test|simplest
  (let [my-project-vars #{#'simplest #'simple #'parallel}
        rv (i/spit-calls-to-tracked-vars file #{#'simplest} my-project-vars #(parallel 1))
        output (slurp file)]
    (is (or
          (= (rest (str/split-lines parallel-tracked-vars-output-1|simplest))
               (rest (str/split-lines output)))
          (= (rest (str/split-lines parallel-tracked-vars-output-2|simplest))
             (rest (str/split-lines output)))))))