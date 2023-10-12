(ns inspector.templates.exporter
  (:require [inspector.core :as core]
            [inspector.templates.common :as common]))

(def fn-call-id (atom 0))
(def level (atom 0))

(defn ^:before-action inc-level-n-id
  [_ _ shared]
  (let [my-call-id @fn-call-id
        my-level @level]
    (swap! fn-call-id inc)
    (swap! level inc)
    (assoc shared :my-call-id my-call-id :my-level my-level)))

(defn prepare-data
  [meta-data fn-args shared]
  (let [thread (Thread/currentThread)
        t-name (.getName thread)
        t-id (.getId thread)]
    {:fn_name    (common/full-name meta-data)
     :fn_args    fn-args
     :level      (:my-level shared)
     :fn_call_id (:my-call-id shared)
     :t_name     t-name
     :t_id       t-id}))

(defn create-export-action
  [exporter-fn]
  (fn ^:action export
    ([meta-data fn-args shared]
     (export meta-data fn-args shared :fn-not-executed-yet))
    ([meta-data fn-args shared return-value]
     (let [d (prepare-data meta-data fn-args shared)]
       (if (= :fn-not-executed-yet return-value)
         (exporter-fn d)
         (exporter-fn (assoc d :fn_return_value return-value))))
     shared)))

(defn ^:after-action dec-level
  [_ _ shared _]
  (swap! level dec)
  shared)

(defn exporter
  [exporter-fn]
  (let [a (create-export-action exporter-fn)]
    (core/create-template [common/always inc-level-n-id
                           common/always a]
                          [common/always a
                           common/always dec-level])))


