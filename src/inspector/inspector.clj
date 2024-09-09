(ns inspector.inspector
  (:require [clojure.set :refer [difference union]]
            [clojure.string :refer [join]]
            [inspector.utils :as utils]
            [inspector.fn-find :as fn-find]
            [inspector.middleware.capture :as capture]
            [inspector.middleware.export :as export]
            [inspector.track :as track])
  (:import java.util.Date))

(defn get-vars
  "Returns all function vars available in namespaces,
   whose string representation matches `regex`."
  [regex]
  (fn-find/get-vars regex))

(def inspector-fn-vars
  (reduce union
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
  (difference vars inspector-fn-vars))

;; Omnipresent mode --------------------------------------------------------------
(defn stream-raw
  [vars export-fn]
  (track/track
    [(partial export/export-middleware export-fn)]
    (remove-inspector-fn-vars vars)))

;; ------------------- --------------------------------------------------------------
(defn parse-opts
  [{:keys [only-start?] :as opts}]
  (let [default {:start       [:fn-args]
                 :only-start? false
                 :end         [:fn-rv]
                 :indent      "|  "
                 :marker      {:start "Г--"
                               :end   "L--"}}
        only-start-opts {:start       [:fn-args :fn-rv]
                         :only-start? true
                         :indent      "   "
                         :marker      {:start "-->"}}]
    (if only-start?
      (merge only-start-opts opts)
      (merge default opts))))

(defn infer-execution-order
  [records]
  (let [; {1 [2 3], 2 [4], 3 [5]}
        adjacency-list (-> (group-by :c-id records)
                           (update-vals #(map :id %)))
        ; 1
        root-id (first (get adjacency-list nil))
        ; [[id status level] ...]
        execution-order (utils/flatten-tree adjacency-list root-id)
        record-map (->> (map #(vector (:id %) %) records)
                        (into {}))]
    [execution-order record-map]))

(defn print-call-hierarchy
  [printer opts records]
  (let [[execution-order record-map] (infer-execution-order records)]

    (printer (str "Time: " (Date.)))
    (let [{:keys [start end indent marker only-start?]} (parse-opts opts)]
      (doseq [[id status level] execution-order]
        (let [record (get record-map id)]
          (if (= status :start)
            (apply
              printer
              (-> (repeat level indent) join (str (:start marker)))
              (:fn-name record)
              (map record start))
            (when-not only-start?
              (apply
                printer
                (-> (repeat level indent) join (str (:end marker)))
                (map record end)))))))))

(defn print-to-file
  [file & args]
  (spit file (str (join " " args) "\n") :append true))

;; Normal mode --------------------------------------------------------------
(defn export-raw
  [vars f]
  (track/with-track
    [{:name       :capture
      :middleware (partial capture/capture-middleware capture/store)
      :store      capture/store}]
    (remove-inspector-fn-vars vars)
    f))

(defn iprint
  [vars f & [opts]]
  (let [{:keys [rv e records]} (export-raw vars f)]
    (print-call-hierarchy println opts (:capture records))
    (if e
      (throw e)
      rv)))

(defn ispit
  [file vars f & [opts]]
  (let [{:keys [rv e records]} (export-raw vars f)]
    (print-call-hierarchy (partial print-to-file file) opts (:capture records))
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

