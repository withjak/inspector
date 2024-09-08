(ns inspector.test.utils-test
  (:require [clojure.test :refer :all]
            [inspector.utils :as utils]))

(deftest flatten-tree-test
  (is (= [[1 :start 0]
          [2 :start 1]
          [4 :start 2]
          [4 :end 2]
          [2 :end 1]
          [3 :start 1]
          [5 :start 2]
          [5 :end 2]
          [3 :end 1]
          [1 :end 0]]
         (utils/flatten-tree
           {1 [2 3]
            2 [4]
            3 [5]}
           1))))
