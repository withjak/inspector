(ns inspector.core)

(defn run-before-rules
  "Executes action if condition evaluates to truthy value."
  [rules meta-data fn-args shared]
  (let [evaluate (fn [shared [condition action]]
                   (if (condition meta-data fn-args shared)
                     (action meta-data fn-args shared)
                     shared))]
    (reduce evaluate shared (partition 2 rules))))

(defn run-after-rules
  "Executes action if condition evaluates to truthy value."
  [rules meta-data fn-args shared return-value]
  (let [evaluate (fn [shared [condition action]]
                   (if (condition meta-data fn-args shared return-value)
                     (action meta-data fn-args shared return-value)
                     shared))]
    (reduce evaluate shared (partition 2 rules))))

(defn create-template
  "Attaches rules (i.e. condition action pairs) before and after execution of a function."
  [before-rules after-rules]
  (fn template
    [var original-fn & args]
    (let [shared (run-before-rules before-rules (meta var) args {})]
      ;; TODO
      ;; IDEA - (apply original-fn (or (:modified-args shared) args))
      ;; USE CASE - when connecting to db, connect to dev db instead of production

      ;; TODO - pass new argument called exception to after actions and conditions
      ;[return-value (try (apply original-fn args)
      ;                   (catch Exception e
      ;                     (run-after-rules after-rules (meta var) args shared {:error (.getMessage e)})
      ;                     (throw e)))]
      (let [return-value (apply original-fn args)]
        (run-after-rules after-rules (meta var) args shared return-value)
        return-value))))

(defn attach-template
  [fn-vars template]
  (fn executor
    [f]
    (with-redefs-fn
      ;; modify all given functions
      (zipmap
        fn-vars
        (map #(partial template % (deref %)) fn-vars))
      ;; run fn f in this modified environment
      #(f))))

;; -------------
(defn redefs-fn-permanent
  [binding-map]
  (doseq [[a-var a-val] binding-map]
    (.bindRoot ^clojure.lang.Var a-var a-val)))

(defn modify-fns-permanent
  [fn-vars template]
  (redefs-fn-permanent
    (zipmap
      fn-vars
      (map #(partial template % (deref %)) fn-vars))))
