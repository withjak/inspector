(ns inspector.core)

; with-redef changes the root binding of vars
; so if a thread calls a fn f,
; then we will execute modified f only if that caller thread explicitly tells us to do so
; by binding *modify-fns* to true
; else we will execute the original f
(def ^:dynamic *modify-fns* false)
(defn get-thread-id
  []
  (.threadId (Thread/currentThread)))

; Dynamic because we need each thread to see only it's self
(def ^:dynamic *state* {:caller-thread-id (get-thread-id) :caller-id 0})
(def id (atom 0))

(defn run-before-rules
  "Executes action if condition evaluates to truthy value."
  [rules meta-data fn-args shared]
  (let [evaluate (fn [shared [condition action]]
                   (if (condition meta-data fn-args shared)
                     (action meta-data fn-args shared)      ;; action must return "shared" (a map)
                     shared))]
    (reduce evaluate shared (partition 2 rules))))

(defn run-after-rules
  "Executes action if condition evaluates to truthy value."
  [rules meta-data fn-args shared return-value]
  (let [evaluate (fn [shared [condition action]]
                   (if (condition meta-data fn-args shared return-value)
                     (action meta-data fn-args shared return-value) ;; action must return "shared" (a map)
                     shared))]
    (reduce evaluate shared (partition 2 rules))))

(defn create-template
  "Attaches rules (i.e. condition action pairs) before and after execution of a function."
  [before-rules after-rules]

  (fn template
    [var original-fn]

    (fn modified-fn
      [& args]
      (if *modify-fns*
        (let [caller-thread-id (:caller-thread-id *state*)
              t-id (get-thread-id)
              c-id (:caller-id *state*)
              my-id (swap! id inc)]

          (binding [*state* (-> *state*
                                (assoc :caller-id my-id) ;; for fns that "f" calls f's id will be their c-id
                                (assoc :caller-thread-id t-id))]
            (let [shared-state {:caller-thread-id caller-thread-id
                                :t-id             t-id
                                :id               my-id
                                :c-id              c-id}      ;; for "f" its c-id it the id of its caller
                  shared (run-before-rules before-rules (meta var) args shared-state)

                  return-value (apply original-fn args)]
              (run-after-rules after-rules (meta var) args shared return-value)
              return-value)))
        (apply original-fn args)))))

(comment
  ;; TODO
  ;; IDEA - (apply original-fn (or (:modified-args shared) args))
  ;; USE CASE - when connecting to db, connect to dev db instead of production

  ;; TODO - pass new argument called exception to after actions and conditions
  ;[return-value (try (apply original-fn args)
  ;                   (catch Exception e
  ;                     (run-after-rules after-rules (meta var) args shared {:error (.getMessage e)})
  ;                     (throw e)))]
  )

(defn attach-template
  [fn-vars template]
  (fn executor
    [f]
    (with-redefs-fn
      ;; modify all given functions
      (zipmap
        fn-vars
        (map #(template % (deref %)) fn-vars))
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
