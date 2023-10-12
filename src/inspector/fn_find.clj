(ns inspector.fn-find)

(defn matching-ns
  "All namespaces whose string representation matches regex."
  [regex]
  (filter #(re-matches regex (str %)) (all-ns)))

(defn is-var-fn?
  [a-var]
  (fn? (deref a-var)))

(defn macro?
  [a-var]
  (get (meta a-var) :macro))

(defn fn-vars-from-ns
  "Returns all var corresponding to function's available in given namespace `ns`"
  [ns]
  (->> (vals (ns-interns ns))
       (filter is-var-fn?)
       (filter #(not (macro? %)))))

(defn get-vars
  "Returns all function vars available in namespaces,
   whose string representation matches `regex`."
  [regex]
  (set
    (apply
      concat
      (map fn-vars-from-ns (matching-ns regex)))))
