(ns inspector.templates.capture
  (:require [clojure.string :refer [join]]
            [inspector.core :as core]
            [inspector.templates.common :as common]
            [inspector.tree :as tree]))

(def accumulator (atom []))

(defn prepare-fn-record
  [meta-data fn-args {:keys [caller-thread-id t-id c-id id]}]
  {:fn-name          (common/full-name meta-data)
   :fn-args          fn-args
   :id               id
   :c-id             c-id
   :t-id             t-id
   :caller-thread-id caller-thread-id})

(defn b-action
  [meta-data fn-args shared]
  (swap! accumulator conj (prepare-fn-record meta-data fn-args shared))
  shared)

(defn a-action
  [meta-data fn-args shared return-value]
  (swap!
    accumulator
    conj
    (assoc (prepare-fn-record meta-data fn-args shared) :fn-rv return-value))
  shared)

(def capture-template
  (core/create-template [common/always b-action] [common/always a-action]))

(defn run
  [vars f]
  (binding [core/*modify-fns* true]
    (let [executor (core/attach-template vars capture-template)
          rv (executor f)
          fn-call-records @accumulator]
      (reset! accumulator [])
      {:rv rv :fn-call-records fn-call-records})))

;; -------------------------------------------------------------------------
;; fns to be used by end user

(defn print-call-hierarchy
  [record-printer fn-call-records]
  (let [rv-records (filter #(contains? % :fn-rv) fn-call-records)

        ; {1 [2 3], 2 [4], 3 [5]}
        id-adjacency-list (update-vals (group-by :c-id rv-records) #(map :id %))
        ; 1
        root-id (first (get id-adjacency-list 0))
        ; [[id status level] ...]
        id-execution-order (tree/flatten-tree id-adjacency-list root-id)

        id-record-map (into {} (map #(vector (:id %) %) rv-records))]
    (doseq [[id status level] id-execution-order]
      (let [{:keys [fn-name fn-args fn-rv]} (get id-record-map id)]
        (if (= status :start)
          (record-printer (str (join (repeat level "|  ")) "Г--") fn-name fn-args)
          (record-printer (str (join (repeat level "|  ")) "L--") fn-rv))))))

(defn print-to-stdout
  [vars f]
  (let [{:keys [rv fn-call-records]} (run vars f)]
    (print-call-hierarchy println fn-call-records)
    rv))

(defn print-to-file
  [file vars f]
  (let [{:keys [rv fn-call-records]} (run vars f)
        printer (fn
                  [& args]
                  (spit file (str (join args) "\n") :append true))]
    (print-call-hierarchy printer fn-call-records)
    rv))

(defn find-calls-to-vars
  [fn-call-records tracked-vars]
  (let [rv-records (filter #(contains? % :fn-rv) fn-call-records)
        id-record-map (into {} (map #(vector (:id %) %) rv-records))
        id-c-id-map (into {} (map #(vector (:id %) (:c-id %)) rv-records))
        fn-name-id-map (->> rv-records
                            (map #(hash-map (:fn-name %) [(:id %)]))
                            (apply merge-with concat))
        tracked-fn-names (set (map #(common/full-name (meta %)) tracked-vars))
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
                                                                       caller-name (common/full-name (meta caller))]
                                                                   (not (contains? tracked-fn-names caller-name))))
                                                               call-chains)
                                           d (map
                                               (fn [call-chain]
                                                 {:call-chain     (map #(get-in id-record-map [% :fn-name]) call-chain)
                                                  :fn-call-records (get id-record-map (last call-chain))})
                                               valid-call-chains)]
                                       (recur (apply conj r d) tfn))
                                     (recur r tfn)))))]
    call-to-tracked-vars))

(defn print-var-calls
  [printer calls]
  (doseq [{:keys [call-chain fn-call-records]} calls]
    (let [{:keys [fn-name fn-args fn-rv]} fn-call-records]
      (printer (join (repeat 20 "♤ ♧ ♡ ♢ ")))
      (printer (str "call-chain: " (vec call-chain)))
      (printer (str "name: " fn-name))
      (printer (str "args: " fn-args))
      (printer (str "rv: " fn-rv)))))

(defn see-call-to-vars
  [tracked-vars vars f]
  (let [{:keys [rv fn-call-records]} (run vars f)
        call-to-tracked-vars (find-calls-to-vars fn-call-records tracked-vars)]
    (print-var-calls println call-to-tracked-vars)
    rv))



