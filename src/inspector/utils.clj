(ns inspector.utils
  (:require [clojure.walk :as walk]))

(defn full-name
  [meta-data]
  (str (:ns meta-data) "/" (:name meta-data)))

(defn prepare-fn-record
  [{:keys [c-tid tid c-id c-chain id uuid time e fn-rv meta-data fn-args]}]
  (merge
    {:fn-name (full-name meta-data)
     :fn-args fn-args
     :id      id
     :tid     tid
     :c-id    c-id
     :c-tid   c-tid
     :c-chain c-chain
     :uuid    uuid
     :time    time
     :fn-rv   fn-rv}
    (when e {:e (Throwable->map e)})))

(defn walk-n-replace
  "Applies f to each non-collection thing.
  Non-collection thing is replaced by the return value."
  [f form]
  (walk/walk
    (partial walk-n-replace f)
    (fn [form]
      (if (coll? form) form (f form)))
    form))

(defn stringify-non-primitives
  [data]
  (let [check-primitive [keyword? number? string? char? nil? boolean? symbol?]
        stringify (fn [form]
                    (cond
                      (some #(% form) check-primitive) form
                      (= (type form) clojure.lang.Atom) (deref form)
                      :else (do
                              (prn :type (type form))
                              (str form))))]
    (walk-n-replace stringify data)))

(comment
  ; to get the datatype map
  (walk-n-replace (fn [form] (vector form (type form))) data))

(defn flatten-tree
  "Returns a depth-first traversal of the tree. Where :start and :end represents
  the start or end of a node's exploration.

  adjacency-list: map => { parent-node  [child child ...], ... }.
  node: this node will be explored using depth first search.

  (flatten-tree
      {1 [2 3]
       2 [4]
       3 [5]}
      1)

  => [[1 :start 0]
      [2 :start 1]
      [4 :start 2]
      [4 :end 2]
      [2 :end 1]
      [3 :start 1]
      [5 :start 2]
      [5 :end 2]
      [3 :end 1]
      [1 :end 0]]

  [node start/end depth]
  node: unique index of each node.
  start/end:
    [[node1 :start] ... [node1 :end]]
    everything in between are children of node1
  level: level of node in the tree
  "
  [adjacency-list node]
  (letfn [(flatten-tree
            [node level]
            (let [children (get adjacency-list node)]
              (concat
                [node :start level]
                (mapcat #(flatten-tree % (inc level)) children)
                [node :end level])))]
    (partition 3 (flatten-tree node 0))))



