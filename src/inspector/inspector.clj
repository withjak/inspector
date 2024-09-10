(ns inspector.inspector
  (:require [clojure.set :as s]
            [inspector.fn-find :as fn-find]
            [inspector.middleware.capture :as capture]
            [inspector.middleware.export :as export]
            [inspector.track :as track]
            [inspector.printer :as printer]))

(defn get-vars
  "Returns all function vars available in namespaces,
   whose string representation matches `regex`."
  [regex]
  (fn-find/get-vars regex))

(def inspector-fn-vars
  (reduce s/union
          (map fn-find/get-vars
               [#"inspector.core"
                #"inspector.fn-find"
                #"inspector.tree"
                #"inspector.utils"
                #"inspector.capture"
                #"inspector.stream"
                #"inspector.inspector"
                #"inspector.test.*"])))

(defn remove-inspector-fn-vars
  [vars]
  (s/difference vars inspector-fn-vars))

;; Omnipresent mode --------------------------------------------------------------
(defn stream-raw
  [vars export-fn]
  (track/track
    [(partial export/export-middleware export-fn)]
    (remove-inspector-fn-vars vars)))

;; Normal mode --------------------------------------------------------------
(defn export-raw
  [vars f]
  (let [store (atom [])
        middlewares [(partial capture/capture-middleware store)]]
    (track/with-track
      middlewares store
      (remove-inspector-fn-vars vars)
      f)))

(defn iprint
  [vars f & [opts]]
  (let [{:keys [rv e records]} (export-raw vars f)]
    (printer/print-call-tree println opts records)
    (if e
      (throw e)
      rv)))

(defn ispit
  [file vars f & [opts]]
  (let [{:keys [rv e records]} (export-raw vars f)]
    (printer/print-call-tree
      (partial printer/print-to-file file)
      opts
      records)
    (if e
      (throw e)
      rv)))

#_(defn export
    "WIP
    Same as `export-raw` with stringify non primitive types present in.
    In progress not complete yet"
    [vars f]
    (let [{:keys [rv records]} (capture/run (remove-inspector-fn-vars vars) f)]
      {:rv rv :records (utils/stringify-non-primitives records)}))


