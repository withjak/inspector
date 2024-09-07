(ns inspector.utils
  (:require [clojure.walk :as walk]))

(def ^:const reset-color "\u001B[0m")

(def ^:const font-colors
  {:BLACK  "\u001B[30m"
   :RED    "\u001B[31m"
   :GREEN  "\u001B[32m"
   :YELLOW "\u001B[33m"
   :BLUE   "\u001B[34m"
   :PURPLE "\u001B[35m"
   :CYAN   "\u001B[36m"
   :WHITE  "\u001B[37m"})

(def ^:const bg-colors
  {;:BLACK  "\u001B[40m"
   :RED    "\u001B[41m"
   :GREEN  "\u001B[42m"
   :YELLOW "\u001B[43m"
   :BLUE   "\u001B[44m"
   :PURPLE "\u001B[45m"
   :CYAN   "\u001B[46m"
   ;:WHITE  "\u001B[47m"
   })

(defn color-font
  [string color]
  (str (get font-colors color) string reset-color))

(defn color-bg
  [string color]
  (str (get bg-colors color) string reset-color))

(defn full-name
  [meta-data]
  (str (:ns meta-data) "/" (:name meta-data)))

(defn prepare-fn-record
  [meta-data fn-args {:keys [c-tid tid c-id c-chain id uuid time e fn-rv] :as shared}]
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
