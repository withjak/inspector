(ns inspector.examples.first
  (:require [inspector.examples.second :as second]
            [inspector.examples.third :as third]
            [inspector.fn-find :as fn-find]
            [inspector.explore :as explore]))

(defn f2
  [a b c d]
  (-> (second/s1 a b c)
      (second/s2 d)
      (third/t1)))

(defn f1
  [a]
  (f2 a (inc a) (dec a) :hello))


(comment
  (f1 1)

  ;; see fn calls
  (def my-project-vars
    "Set of functions that you want to modify in some way."
    (fn-find/get-vars #"inspector.examples.*"))

  (explore/show-all-calls my-project-vars #(f1 1))

  ;; See calls happening to a particular namespace / group of vars
  (def db-namespace-vars
    (fn-find/get-vars #"inspector.examples.third"))
  (def my-project-vars
    (clojure.set/difference my-project-vars db-namespace-vars))

  (explore/show-cross-group-calls {:my-proj  my-project-vars
                                   :db-calls db-namespace-vars}
                                  #(f1 1))

  ;; export data
  (explore/export "/tmp/data.json" my-project-vars #(f1 1))
  (explore/export println {} my-project-vars #(f1 1)))