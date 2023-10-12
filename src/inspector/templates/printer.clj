(ns inspector.templates.printer
  (:require [inspector.core :as core]
            [inspector.templates.common :as common]
            [clojure.string :refer [join]]))

(def level (atom 0))

(defn ^:before-action before-printer
  [meta-data fn-args _]
  (let [prefix       (str (join (repeat @level "|  ")) "Г--")
        bg-colors    (vals common/bg-colors)
        color        (nth bg-colors (mod @level (count bg-colors)))
        colored-name (str color (common/full-name meta-data) common/reset-color)]
    (println
      prefix
      colored-name
      fn-args))
  (swap! level inc))

(defn ^:after-action after-printer
  [_ _ _ return-value]
  (swap! level dec)
  (println (str (join (repeat @level "|  ")) "L__") return-value)
  )

(def printer
  (core/create-template [common/always before-printer]
                        [common/always after-printer]))


