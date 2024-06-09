(ns inspector.core)

(defn run-rules
  "Executes action if condition evaluates to truthy value."
  [rules meta-data fn-args shared]
  (let [evaluate (fn [shared [condition action]]
                   (if (condition meta-data fn-args shared)
                     (action meta-data fn-args shared)      ;; action must return "shared" (a map)
                     shared))]
    (reduce evaluate shared (partition 2 rules))))

(defn get-thread-id
  []
  (let [t (Thread/currentThread)]
    (try (.threadId t)
         (catch Exception e (.getId t)))))

; when set as true, using bindings, then data for all fns running in that thread (and threads it spawns) will be tracked.
(def ^:dynamic *modify-fns* false)
; when set as true, the data of all fns running across all threads will be tracked.
(def modify-all (atom false))

; *state* is used by modified fn
; to know it's caller's information and
; then updating *state* to pass-down its own information to its children.
; This works because
; *state* is dynamic, because dynamic is thread local. And in a thread execution is sequential.
; more generally think of any information that need to be shared in a thread and all its children threads.
(def ^:dynamic *state* nil)
(def id (atom 0))

(comment
  ; TODO: Explore
  ; get rid of atom "id". and start with *state* = nil
  ; in create-template check if *state* = nil then
  ; {:caller-thread-id (get-thread-id) :caller-id 0} or
  ; also add :caller-id-chain = [] and
  ; when no caller, then add #uuid as first entry, this will be good for traceability.
  ; or may be add :uuid as a field.
  ; also think about at what time is inspector injected into the system, when applying inspector globally.
  ; 1. Every ns is loaded and now injecting inspector, like running it at the end of -main
  ;    This will let every long running thread its own #uuid, i.e. for e.g. all handlers will have unique #uuid
  ; 2. Injecting before any long running threads are spawned. then only the main thread will have #uuid.
  ; Maybe then it should be mentioned as best practice to inject inspector for global monitoring
  ; either after every component has been loaded (either in source code or via repl)
  )

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
         (let [{:keys [id tid uuid c-chain] :as shared-state} {:c-chain (or (:c-chain *state*) [])
                                                               :c-tid   (:c-tid *state*) ; when this is the first fn in this thread to be executed
                                                               :c-id    (:c-id *state*) ; for "f" its c-id is the id of its caller
                                                               :uuid    (or (:uuid *state*) (random-uuid))
                                                               :tid     (get-thread-id) ; tid is sufficient to deduce :caller-thread-id, but still keeping :caller-thread-id as it easier to so.
                                                               :id      (swap! id inc)}]
           ;; for fns that "f" calls, f's id will be their c-id
           (binding [*state* {:c-id id :c-tid tid :uuid uuid :c-chain (conj c-chain id)}]
             (let [shared (run-rules before-rules meta-data args shared-state)
                   start-time (nano-time)
                   {:keys [rv e]} (try
                                    {:rv (apply fn-value args)}
                                    (catch Exception e
                                      {:e e}))
                   execution-time (- (nano-time) start-time)
                   shared (assoc shared :execution-time execution-time :fn-rv rv :e e)]
               (run-rules after-rules meta-data args shared)
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
    (when-let [original-value (:inspector-original-value (meta fn-var))]
      (alter-meta! fn-var dissoc :inspector-original-value)
      (alter-var-root fn-var (fn [_] original-value)))))

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
