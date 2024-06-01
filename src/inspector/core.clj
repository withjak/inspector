(ns inspector.core)

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

(defn get-thread-id
  []
  (let [t (Thread/currentThread)]
    (try (.threadId t)
      (catch Exception e (.getId t)))))

(def ^:dynamic *modify-fns* false)
; Dynamic because we need each thread to see only it's self
(def ^:dynamic *state* {:caller-thread-id (get-thread-id) :caller-id 0})
(def id (atom 0))

(defn nano-time
  []
  (. System (nanoTime)))

(defn create-template
  "Attaches rules (i.e. condition action pairs) before and after execution of a function."
  [before-rules after-rules]

  (fn template
    [fn-var]
    ; value is the original function
    (let [fn-value (deref fn-var)
          meta-data (meta fn-var)]

      (fn modified-value
        [& args]
        (if *modify-fns*
          (let [{:keys [id t-id] :as shared-state} {:caller-thread-id (:caller-thread-id *state*)
                                                    :t-id             (get-thread-id)
                                                    :id               (swap! id inc)
                                                    ;; for "f" its c-id is the id of its caller
                                                    :c-id             (:caller-id *state*)}]
            (binding [*state* (-> *state*
                                  (assoc :caller-id id) ;; for fns that "f" calls f's id will be their c-id
                                  (assoc :caller-thread-id t-id))]
              (let [shared (run-before-rules before-rules meta-data args shared-state)
                    start-time (nano-time)
                    return-value (apply fn-value args)
                    shared (assoc shared :execution-time (- (nano-time) start-time))]
                (run-after-rules after-rules meta-data args shared return-value)
                return-value)))
          (apply fn-value args))))))

(defn attach-template
  [fn-vars template]
  (fn executor
    [f]
    (with-redefs-fn
      ;; modify all given functions
      (zipmap fn-vars (map template fn-vars))
      ;; run fn f in this modified environment
      #(f))))

(comment
  ;; TODO
  ;; IDEA - (apply original-fn (or (:modified-args shared) args))
  ;; USE CASE - when connecting to db, connect to dev db instead of production

  ;; TODO - pass new argument called exception to after actions and conditions
  ;[return-value (try (apply original-fn args)
  ;                   (catch Exception e
  ;                     (run-after-rules after-rules (meta var) args shared {:error (.getMessage e)})
  ;                     (throw e)))]

  ; TODO: In case an un caught exception occurs, the stack trace is bad, its filled with lambda functions.
  ; Fix:
  ; 1. Simple: Catch exception, print fn details using its var, raise the exception.
  ; 2. Might be complex: see how clojure creates stacktrace, and is there a way to modify it.
  )

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

(comment
  ; can you call a symbol
  ; var?
  ; value?

  (defn foo
    [a]
    (/ 1 a))

  (defn type-of-thing
    [thing]
    (cond
      (symbol? thing) :symbol
      (var? thing) :var
      :else :value))
  (map type-of-thing [foo #'foo 'foo])
  (map type [foo #'foo 'foo])

  ; calling
  (#'foo :a)
  (foo :a)
  ('foo :a)                                                 ; ??
  ; interesting
  ; https://clojure.org/reference/data_structures#Symbols
  ; sumbols are just like keywords
  ('foo {'foo 1})
  (:a nil)

  ;; Conclusion
  ; Function call can be made using either value or var.
  ; Symbol cant be used to make a function call directly. See fn resolve
  ; -------------------------------

  ; can we change the value of var in way that metadata of var is not affected?
  (meta (var foo))
  (meta #'foo)

  (with-redefs-fn {#'foo (fn [x] :a)}
    (fn []
      (println (foo 1))
      ; proves that binding is actually changes
      (println (('foo (ns-interns *ns*)) 1))
      ; but metadata has not changes
      (meta ('foo (ns-interns *ns*)))))
  :conclusion
  ; metadata stays unchanged when using with-redefs-fn
  )
