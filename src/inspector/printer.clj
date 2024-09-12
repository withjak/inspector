(ns inspector.printer
  (:require [clojure.string :as str])
  (:import java.util.Date))

(defn flatten-tree
  "Returns a depth-first traversal of the tree. Where :start and :end represents
  the start or end of a node's exploration.

  adjacency-list: map => { parent-node  [child child ...], ... }.
  node: this node will be explored using depth first search.

  (flatten-tree
      {1 [2 3]
       2 [4]
       3 [5]}
      1)

  => [[1 :start 0]
      [2 :start 1]
      [4 :start 2]
      [4 :end 2]
      [2 :end 1]
      [3 :start 1]
      [5 :start 2]
      [5 :end 2]
      [3 :end 1]
      [1 :end 0]]

  [node start/end depth]
  node: unique index of each node.
  start/end:
    [[node1 :start] ... [node1 :end]]
    everything in between are children of node1
  depth: depth of node in the tree
  "
  [adjacency-list node]
  (letfn [(flatten-tree
            [node depth]
            (let [children (get adjacency-list node)]
              (concat
                [node :start depth]
                (mapcat #(flatten-tree % (inc depth)) children)
                [node :end depth])))]
    (partition 3 (flatten-tree node 0))))

(defn parse-opts
  [opts]
  ; Possible keywords in :start and :end:
  ; :fn-name :fn-args :fn-rv :e :id :tid :c-id :c-tid :c-chain :time :uuid
  (let [expanded-view? (if (contains? opts :expanded-view?)
                         (:expanded-view? opts)
                         true)
        expanded-view {:start          [:fn-name :fn-args]
                       :expanded-view? true
                       :end            [:fn-rv]
                       :indent         "|  "
                       :marker         {:start "Г--"
                                        :end   "L--"}}
        collapsed-view {:start          [:fn-name :fn-args :fn-rv]
                        :expanded-view? false
                        :indent         "   "
                        :marker         {:start "-->"}}]
    (if expanded-view?
      (merge expanded-view opts)
      (merge collapsed-view opts))))

(defn get-indicator
  "Returns
  Г--
  L--
  |  Г--
  |  L--
  -->
     -->"
  [depth indent marker exploration]
  (str (str/join (repeat depth indent)) (exploration marker)))

(defn skip-escape-sequences
  [record k]
  (if (or (= k :fn-args) (= k :fn-rv))
    (with-out-str
      (pr (k record)))
    (k record)))

(defn format-values
  "`fn-args` and `fn-rv` might contain strings with escape sequences such as \n.
  They need to be properly escaped, else the output spans to multiple lines."
  [record opts exploration]
  (map (partial skip-escape-sequences record) (exploration opts)))

(defn create-line
  [[_ exploration depth]
   {:keys [indent marker] :as opts}
   record]
  (flatten
    [(get-indicator depth indent marker exploration)
     (format-values record opts exploration)]))

(defn infer-execution-order
  [records]
  (let [adjacency-list (-> (group-by :c-id records)
                           (update-vals #(map :id %)))      ; {1 [2 3], 2 [4]}
        root (first (get adjacency-list nil))]
    (flatten-tree adjacency-list root)))

(defn print-call-tree
  [printer opts records]
  (let [dft (infer-execution-order records)
        record-map (->> (map #(vector (:id %) %) records)
                        (into {}))]

    (printer (str "Time: " (Date.)))
    (let [{:keys [expanded-view?] :as opts} (parse-opts opts)]

      (doseq [node dft]
        (let [[id exploration _] node
              record (get record-map id)]
          (if (= exploration :start)
            (apply printer (create-line node opts record))
            (when expanded-view?
              (apply printer (create-line node opts record)))))))))

(defn print-to-file
  [file & args]
  (spit file (str (str/join " " args) "\n") :append true))
