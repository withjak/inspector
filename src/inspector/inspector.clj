(ns inspector.inspector
  (:require [clojure.set :refer [difference union]]
            [clojure.string :refer [join]]
            [inspector.utils :as utils]
            [inspector.fn-find :as fn-find]
            [inspector.capture :as capture]
            [inspector.tree :as tree])
  (:import java.util.Date))

(defn handle-nil-c-id
  "Hack so that I do not have to change code that
  assumes that 0 is the c-id of the first caller
  "
  [fn-call-records]
  (map
    (fn [{:keys [c-id] :as m}]
      (if (nil? c-id)
        (assoc m :c-id 0)
        m))
    fn-call-records))

(defn print-call-hierarchy
  [printer fn-call-records]
  (let [fn-call-records (handle-nil-c-id fn-call-records)
        rv-records (filter #(contains? % :fn-rv) fn-call-records)

        ; {1 [2 3], 2 [4], 3 [5]}
        id-adjacency-list (update-vals (group-by :c-id rv-records) #(map :id %))
        ; 1
        root-id (first (get id-adjacency-list 0))
        ; [[id status level] ...]
        id-execution-order (tree/flatten-tree id-adjacency-list root-id)

        id-record-map (into {} (map #(vector (:id %) %) rv-records))]
    (printer (str "Time: " (Date.)))
    (doseq [[id status level] id-execution-order]
      (let [{:keys [fn-name fn-args fn-rv]} (get id-record-map id)]
        (if (= status :start)
          (printer (str (join (repeat level "|  ")) "Г--") fn-name fn-args)
          (printer (str (join (repeat level "|  ")) "L--") fn-rv))))))

(defn print-to-file
  [file & args]
  (spit file (str (join " " args) "\n") :append true))

(defn find-calls-to-vars
  [fn-call-records tracked-vars]
  (let [fn-call-records (handle-nil-c-id fn-call-records)
        rv-records (filter #(contains? % :fn-rv) fn-call-records)
        id-record-map (into {} (map #(vector (:id %) %) rv-records))
        id-c-id-map (into {} (map #(vector (:id %) (:c-id %)) rv-records))
        fn-name-id-map (->> rv-records
                            (map #(hash-map (:fn-name %) [(:id %)]))
                            (apply merge-with concat))
        tracked-fn-names (set (map #(utils/full-name (meta %)) tracked-vars))
        call-to-tracked-vars (loop [r []
                                    [fn-name & tfn] tracked-fn-names]
                               (if (nil? fn-name)
                                 r
                                 (let [id-s (get fn-name-id-map fn-name)]
                                   (if id-s
                                     (let [call-chains (map (partial tree/find-path id-c-id-map) id-s)
                                           valid-call-chains (filter
                                                               (fn [call-chain]
                                                                 (let [[caller _] (take-last 2 call-chain)
                                                                       caller-name (:fn-name (get id-record-map caller))]
                                                                   (not (contains? tracked-fn-names caller-name))))
                                                               call-chains)
                                           d (map
                                               (fn [call-chain]
                                                 {:call-chain      (map #(get-in id-record-map [% :fn-name]) call-chain)
                                                  :fn-call-records (get id-record-map (last call-chain))})
                                               valid-call-chains)]
                                       (recur (apply conj r d) tfn))
                                     (recur r tfn)))))]
    call-to-tracked-vars))

(defn print-var-calls
  [printer calls]
  (printer (str "Time: " (Date.)))
  (doseq [{:keys [call-chain fn-call-records]} calls]
    (let [{:keys [fn-name fn-args fn-rv]} fn-call-records]
      (printer (join (repeat 20 "♤ ♧ ♡ ♢ ")))
      (printer (str "call-chain: " (vec call-chain)))
      (printer (str "name: " fn-name))
      (printer (str "args: " fn-args))
      (printer (str "rv: " fn-rv)))))

(def inspector-fn-vars
  (reduce union
          (map fn-find/get-vars
               [#"inspector.core"
                #"inspector.fn-find"
                #"inspector.tree"
                #"inspector.utils"
                #"inspector.capture"
                #"inspector.inspector"
                #"inspector.test.*"])))

(defn remove-inspector-fn-vars
  [vars]
  (difference vars inspector-fn-vars))

;; -------------------------------------------------------------------------
;; fns to be used by end user

(defn print-captured-data
  [vars f]
  (let [{:keys [rv fn-call-records]} (capture/run (remove-inspector-fn-vars vars) f)]
    (print-call-hierarchy println fn-call-records)
    rv))

(defn spit-captured-data
  [file vars f]
  (let [{:keys [rv fn-call-records]} (capture/run (remove-inspector-fn-vars vars) f)]
    (print-call-hierarchy (partial print-to-file file) fn-call-records)
    rv))

(defn print-calls-to-tracked-vars
  [tracked-vars vars f]
  (let [{:keys [rv fn-call-records]} (capture/run (remove-inspector-fn-vars vars) f)
        call-to-tracked-vars (find-calls-to-vars fn-call-records tracked-vars)]
    (print-var-calls println call-to-tracked-vars)
    rv))

(defn spit-calls-to-tracked-vars
  [file tracked-vars vars f]
  (let [{:keys [rv fn-call-records]} (capture/run (remove-inspector-fn-vars vars) f)
        call-to-tracked-vars (find-calls-to-vars fn-call-records tracked-vars)]
    (print-var-calls (partial print-to-file file) call-to-tracked-vars)
    rv))


;(defn show-all-calls-perm
;  [vars]
;  (core/modify-fns-permanent (remove-inspector-fn-vars vars) printer/printer))

;(defn export-perm
;  [filename vars]
;  (core/modify-fns-permanent
;    (remove-inspector-fn-vars vars)
;    (exporter/exporter (partial exporter-fn filename))))

