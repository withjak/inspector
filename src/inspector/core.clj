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
(def modify-all (atom false))

; *state* is used by modified fn
; to know it's caller's information and
; then updating *state* to pass-down its own information to its children.
; This works because
; *state* is dynamic, because dynamic is thread local. And in a thread execution is sequential.
(def ^:dynamic *state* {:caller-thread-id (get-thread-id) :caller-id 0})
; :caller-id 0 might be an issue in case process spans multiple threads at the start
; issue like it caller-id = 0 wont be unique anymore. But :caller-thread-id :caller-id combination will still be unique
; Also think what will happen when when we alter root bindings permanently
(def id (atom 0))

(defn nano-time
  []
  (. System (nanoTime)))

(defn create-template
  "Attaches rules (i.e. condition action pairs) before and after execution of a function."
  [before-rules after-rules]

  (fn template
    ([fn-var]
     (template (deref fn-var) (meta fn-var)))
    ([fn-value meta-data]
     ; value is the original function
     (fn modified-value
       [& args]
       (if (or modify-all *modify-fns*)
         (let [{:keys [id t-id] :as shared-state} {:caller-thread-id (:caller-thread-id *state*)
                                                   ; t-id is sufficient to deduce :caller-thread-id, but still keeping :caller-thread-id as it easier to so.
                                                   :t-id             (get-thread-id)
                                                   :id               (swap! id inc)
                                                   ;; for "f" its c-id is the id of its caller
                                                   :c-id             (:caller-id *state*)}]
           ;; for fns that "f" calls, f's id will be their c-id
           (binding [*state* {:caller-id id
                              ; :caller-id-chain (conj (:caller-id *state*) id)
                              :caller-thread-id t-id}]
             (let [shared (run-before-rules before-rules meta-data args shared-state)
                   start-time (nano-time)
                   {:keys [rv e]} (try
                                    {:rv (apply fn-value args)}
                                    (catch Exception e
                                      {:e e}))
                   shared (assoc shared :execution-time (- (nano-time) start-time)
                                        :e e)]
               (run-after-rules after-rules meta-data args shared rv)
               (if e
                 (throw e)
                 rv))))
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
(defn attach-template-permanent
  [fn-vars template]
  (doseq [fn-var fn-vars]
    ; better, so that no nested templates
    ; (when-not (or (:inspector-skip (meta fn-var)) (:inspector-original-value (meta fn-var)) ))
    (when-not (:inspector-skip (meta fn-var))
      ; attach the original value in meta, so it could be recovered if needed
      (alter-meta! fn-var assoc :inspector-original-value (deref fn-var))
      (alter-var-root fn-var (fn [fn-value] (template fn-value (meta fn-var)))))))

(defn restore-original-value
  [fn-vars]
  (doseq [fn-var fn-vars]
    (when-let [orignial-value (:inspector-original-value (meta fn-var))]
      (alter-meta! fn-var dissoc :inspector-original-value)
      (alter-var-root fn-var (fn [_] orignial-value)))))

(comment
  (defn foo
    [a]
    a)

  (let [foo-var #'foo
        foo-value (deref #'foo)]
    (alter-var-root foo-var (fn [_]
                              (prn (type foo-value))
                              (fn [n]
                                (println "Squaring" n)
                                (foo-value n)))))
  (foo 1)

  (def ^{:version 1} document "This is text")
  (meta #'document)
  (alter-meta! #'document assoc :yo 123)
  (meta #'document))

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
  ; symbols are just like keywords
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
