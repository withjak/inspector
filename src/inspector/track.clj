(ns inspector.track
  (:require [inspector.core :as core]))

; normal mode
(defn with-track
  [middlewares vars f]
  #_{:pre [(every? keyword? (map :name middlewares))
           (every? #(= clojure.lang.Atom (type %)) (map :store middlewares))
           (every? fn? (map :middleware middlewares))]}

  (let [m (map :middleware middlewares)
        {:keys [rv e]} (try {:rv (core/with-modify-fns vars f m)}
                            (catch Exception e
                              {:e e}))
        records (->> (filter :store middlewares)
                     (map #(vector (:name %) @(:store %)))
                     (into {}))]
    (doseq [store (map :store middlewares)]
      (reset! store []))
    {:rv rv :e e :records records}))

; omnipresent mode
(defn track
  [middlewares vars]
  (reset! core/modify true)
  (core/alter-fns vars middlewares))

(defn un-track
  [vars]
  (reset! core/modify false)
  (core/restore-altered-fns vars))
