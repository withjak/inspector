(ns inspector.track
  (:require [inspector.core :as core]))

; normal mode
(defn with-track
  "Arguments:
  `store`: A place to save data for normal mode middlewares.
           As we would generally want to so some processing on the data after execution of function f.
  `middlewares`: vector of middlewares, which may or may not save data in `store`."
  [middlewares store vars f]
  (let [{:keys [rv e]} (try
                         {:rv (core/with-modify-fns vars f middlewares)}
                         (catch Exception e
                           {:e e}))]
    {:rv rv :e e :records @store}))

; omnipresent mode
(defn track
  [middlewares vars]
  (reset! core/modify true)
  (core/alter-fns vars middlewares))

(defn un-track
  [vars]
  (reset! core/modify false)
  (core/restore-altered-fns vars))


