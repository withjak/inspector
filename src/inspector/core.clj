(ns inspector.core)

(defn run-actions
  "Executes action if condition evaluates to truthy value."
  [actions meta-data fn-args shared]
  (reduce
    (fn [shared action]
      (action meta-data fn-args shared)) ;; action must return "shared" (a map)
    shared actions))

(defn get-thread-id
  []
  (let [t (Thread/currentThread)]
    (try (.threadId t)
         (catch Exception e (.getId t)))))

(def ^:dynamic *modify-fns*
  "When set as true - the data of all fns running in that thread (and threads it spawns) will be tracked."
  false)

(def modify-all
  "When set as true - the data of all fns running across all threads will be tracked."
  (atom false))

; *state* is dynamic, dynamic is thread local. And in a thread execution is sequential.
(def ^:dynamic *state*
  "Contains any information that need to be shared in a thread and all its children threads."
  nil)

(def id
  "Unique identifier for each function call.
  Same fn called with same arguments, will be assigned different id each time its called."
  (atom 0))

(defn nano-time
  []
  (. System (nanoTime)))

(defn create-template
  "Attaches actions before and after execution of a function."
  [before-actions after-actions]

  ^{:doc "Return new value which replaces the original value pointed to by function's var"}
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
             (let [shared (run-actions before-actions meta-data args shared-state)
                   start-time (nano-time)
                   {:keys [rv e]} (try
                                    {:rv (apply fn-value args)}
                                    (catch Exception e
                                      {:e e}))
                   time (- (nano-time) start-time)
                   shared (assoc shared :time time :fn-rv rv :e e)]
               (run-actions after-actions meta-data args shared)
               (if e
                 (throw e)
                 rv))))
         (apply fn-value args))))))

(defn attach-template
  [fn-vars template]

  ^{:doc "In context of current thread (and any children it spawns), modify `fn-vars` and then call `f` in this modified environment."}
  (fn executor
    [f]
    (with-redefs-fn
      ;; modify all given functions
      (zipmap fn-vars (map template fn-vars))
      ;; run fn f in this modified environment
      #(f))))

(defn attach-template-permanent
  "Alter root binding of `fn-vars` to point to new value which is a wrapper over the original value"
  [fn-vars template]
  (doseq [fn-var fn-vars]
    ; better, so that no nested templates
    ; but seems like there is a bug here or in the test
    ; (when-not (or (:i-skip (meta fn-var)) (:i-original-value (meta fn-var)) ))
    (when-not (:i-skip (meta fn-var))
      ; attach the original value in meta, so it could be recovered if needed
      (alter-meta! fn-var assoc :i-original-value (deref fn-var))
      (alter-var-root fn-var (fn [fn-value] (template fn-value (meta fn-var)))))))

(defn restore-original-value
  [fn-vars]
  (doseq [fn-var fn-vars]
    (when-let [original-value (:i-original-value (meta fn-var))]
      (alter-meta! fn-var dissoc :i-original-value)
      (alter-var-root fn-var (fn [_] original-value)))))

(comment
  ; can you call a:
  ; symbol?
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
