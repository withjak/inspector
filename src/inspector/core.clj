(ns inspector.core)

(defn get-thread-id
  []
  (let [t (Thread/currentThread)]
    (try (.threadId t)
         (catch Exception e (.getId t)))))

(def ^:dynamic *modify*
  "Visible only to current thread (and threads it spawns)"
  false)

(def modify
  "Visible to all threads"
  (atom false))

; *state* is dynamic, dynamic is thread local. And in a thread execution is sequential.
(def ^:dynamic *state*
  "Contains information that need to be shared in a thread and all its children threads."
  nil)

(def id
  "Unique identifier for each function call.
  Same fn called with same arguments, will be assigned different id each time its called."
  (atom 0))

(defn nano-time
  []
  (. System (nanoTime)))

(defn handler
  [{:keys [fn-value fn-args] :as state}]
  (let [start-time (nano-time)
        {:keys [rv e]} (try
                         {:rv (apply fn-value fn-args)}
                         (catch Exception e
                           {:e e}))
        time (- (nano-time) start-time)
        new-state (assoc state :time time :fn-rv rv :e e)]
    new-state))

(defn get-handler
  [middlewares]
  ((apply comp middlewares) handler))

(defn get-modified-fn
  "Return new value which replaces the original value pointed to by function's var"
  ([handler fn-var]
   (get-modified-fn handler (deref fn-var) (meta fn-var)))
  ([handler fn-value fn-meta]
   ; fn-value is the original function value
   (fn modified-value
     [& args]
     (if (or @modify *modify*)
       (let [{:keys [id tid uuid c-chain] :as shared-state} {:c-chain  (or (:c-chain *state*) [])
                                                             :c-tid    (:c-tid *state*) ; c-id is sufficient to deduce c-tid, keeping it anyway.
                                                             :c-id     (:c-id *state*)
                                                             :uuid     (or (:uuid *state*) (random-uuid))
                                                             :tid      (get-thread-id)
                                                             :id       (swap! id inc)

                                                             :fn-args  args
                                                             :fn-value fn-value
                                                             :fn-meta  fn-meta}]
         ;; for fns that "f" calls, f's id will be their c-id
         (binding [*state* {:c-id id :c-tid tid :uuid uuid :c-chain (conj c-chain id)}]
           (let [{:keys [fn-rv e]} (handler shared-state)]
             (if e
               (throw e)
               fn-rv))))
       (apply fn-value args)))))

(defn with-modify-fns
  "In context of current thread (and any children it spawns),
  modify `fn-vars` and then call `f` in this modified environment."
  [fn-vars f middlewares]
  (let [handler (get-handler middlewares)]
    (binding [*modify* true]
      (with-redefs-fn
        (zipmap fn-vars
                (map (partial get-modified-fn handler) fn-vars)) ;; modify all given functions
        f))))

(defn alter-fns
  "Alter root binding of `fn-vars` to point to new value which is a wrapper over the original value"
  [fn-vars middlewares]
  (let [handler (get-handler middlewares)]
    (doseq [fn-var fn-vars]
      (when-not (or (:i-skip (meta fn-var))
                    (:i-original (meta fn-var)))
        (alter-meta! fn-var assoc :i-original (deref fn-var)) ; for restoring vars if needed
        (alter-var-root fn-var (fn [fn-value] (get-modified-fn handler fn-value (meta fn-var))))))))

(defn restore-altered-fns
  [fn-vars]
  (doseq [fn-var fn-vars]
    (when-let [original-value (:i-original (meta fn-var))]
      (alter-meta! fn-var dissoc :i-original)
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
