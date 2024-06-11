(ns inspector.test.stream-2nd-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]
            [inspector.stream :as stream])
  (:import [clojure.lang PersistentQueue]))

(defn queue
  "Create a new stateful queue"
  []
  (ref clojure.lang.PersistentQueue/EMPTY))

(defn enqueue
  [q message]
  (dosync
    (alter q conj message)))

(defn dequeue
  [q]
  (dosync
    (let [item (peek @q)]
      (alter q pop)
      item)))

(defn start-consumer
  [q handler-fn]
  (future
    (loop []
      (when-let [message (dequeue q)]
        (handler-fn message))
      (recur))))

(defn start-producer
  [q messages]
  (future
    (Thread/sleep 5000)
    (doseq [m messages]
      (enqueue q m))))

(defn foo
  [m] m)

(defn handler
  [message]
  [(foo message) :ok])

(deftest foo-test
  (let [q (queue)
        handler-thread-1 (start-consumer q handler)
        handler-thread-2 (start-consumer q handler)
        streamed-data (atom [])
        store-data (fn [data] (swap! streamed-data conj data))
        my-project-vars #{#'handler #'foo}]
    (start-producer q [:1 :2 :3])
    (stream/start-streaming my-project-vars store-data)
    (Thread/sleep 10000)
    (is (= 3 (count (filter :fn-rv @streamed-data))))
    (future-cancel handler-thread-1)
    (future-cancel handler-thread-2)
    (core/restore-original-value #{#'handler #'foo})))
