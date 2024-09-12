(ns inspector.fn-find)

(defn matching-ns
  "All namespaces whose string representation matches regex."
  [regex]
  (->> (all-ns)
       (filter #(re-matches regex (str %)))))

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
       (remove macro?)))

(defn get-vars
  "Returns all function vars available in namespaces,
   whose string representation matches `regex`."
  [regex]
  (->> (matching-ns regex)
       (mapcat fn-vars-from-ns)
       set))
