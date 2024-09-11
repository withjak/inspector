(ns inspector.utils
  (:require [clojure.walk :as walk]))

(defn full-name
  [fn-meta]
  (str (:ns fn-meta) "/" (:name fn-meta)))

(defn prepare-fn-record
  [{:keys [c-tid tid c-id c-chain id uuid time e fn-rv fn-meta fn-args]}]
  (merge
    {:fn-name (full-name fn-meta)
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

#_(defn walk-n-replace
    "Applies f to each non-collection thing.
    Non-collection thing is replaced by the return value."
    [f form]
    (walk/walk
      (partial walk-n-replace f)
      (fn [form]
        (if (coll? form) form (f form)))
      form))

#_(defn stringify-non-primitives
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
