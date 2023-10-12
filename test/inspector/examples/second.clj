(ns inspector.examples.second
  (:require [inspector.examples.third :as third]))

(defn s1
  [a b c]
  (third/t1 (+ a b c)))

(defn s2
  [a d]
  (if (= d :hello)
    (inc a)
    a))

