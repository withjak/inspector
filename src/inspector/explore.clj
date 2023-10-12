(ns inspector.explore
  (:require [clojure.set :refer [difference union]]
            [inspector.core :as core]
            [inspector.fn-find :as fn-find]
            [inspector.templates.printer :as printer]
            [inspector.templates.exporter :as exporter]
            [inspector.templates.cross_group :as cross-group]
            [inspector.utils :refer [update-vals]]
            [cheshire.core :refer [generate-string]]
            [cheshire.generate :refer [add-encoder encode-str]]))

(def interceptor-fn-vars
  (reduce union
          (map fn-find/get-vars
               [#"inspector.explore"
                #"inspector.core"
                #"inspector.fn-find"
                #"inspector.templates.*"
                #"inspector.test.*"])))

(defn remove-interceptor-fn-vars
  [vars]
  (difference vars interceptor-fn-vars))

;; ------------
(defn show-all-calls
  [vars f]
  ((core/attach-template
     (remove-interceptor-fn-vars vars)
     printer/printer) f))

(defn show-all-calls-perm
  [vars]
  (core/modify-fns-permanent (remove-interceptor-fn-vars vars) printer/printer))

;; ------------
(defn default-exporter-fn
  [{:keys [filename] :as args-map} json]
  (add-encoder java.lang.Object encode-str)
  (spit filename (str (generate-string json) "\n") :append true))

(defn export
  ([filename vars f]
   (export default-exporter-fn {:filename filename} vars f))
  ([exporter-fn exporter-fn-args vars f]
   ((core/attach-template
      (remove-interceptor-fn-vars vars)
      (exporter/exporter (partial exporter-fn exporter-fn-args)))
    f)))

;(defn export-perm
;  [filename vars]
;  (core/modify-fns-permanent
;    (remove-interceptor-fn-vars vars)
;    (exporter/exporter (partial exporter-fn filename))))

;; -----------------
(defn show-cross-group-calls
  [groups f]
  (let [groups (update-vals groups remove-interceptor-fn-vars)]
    ((core/attach-template
       (reduce union (vals groups))
       (cross-group/cross-group-call groups))
     f)))


