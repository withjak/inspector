(ns inspector.test.core-test
  (:require [clojure.test :refer :all]
            [inspector.core :as core]))

; get-modified-fn tests ---------------------------------------------

(defn foo [x] x)
(defn foo-error [x] (/ x 0))

(deftest handler-test
  (testing "function get executed and return value is returned"
    (let [new-state (core/handler {:fn-value foo :fn-args (list [1 2])})
          k (set (keys new-state))]
      (is (contains? k :time))
      (is (contains? k :e))
      (is (contains? k :fn-rv))
      (is (= [1 2] (:fn-rv new-state)))
      (is (= nil (:e new-state)))))

  (testing "function get executed and error is returned"
    (let [new-state (core/handler {:fn-value foo-error :fn-args (list 2)})
          k (set (keys new-state))]
      (is (contains? k :time))
      (is (contains? k :e))
      (is (contains? k :fn-rv))
      (is (= nil (:fn-rv new-state)))
      (is (= "Divide by zero" (-> (:e new-state)
                                  Throwable->map
                                  :cause))))))

(defn middleware
  [store handler]
  (fn [state]
    (let [new-state (handler state)]
      (swap! store conj new-state)
      new-state)))

(deftest get-modified-fn-test-no-modification
  (let [store (atom [])
        middlewares [(partial middleware store)]
        new-fn (core/get-modified-fn middlewares #'foo)]

    (is (= [1 2] (new-fn [1 2])))
    (is (= [] @store)))

  (let [store (atom [])
        middlewares [(partial middleware store)]
        new-fn (core/get-modified-fn middlewares #'foo-error)]

    ; not sure why but clojure.test thrown? is not being resolved
    (is (= "Divide by zero" (try (new-fn 1)
                                 (catch ArithmeticException e
                                   (-> e Throwable->map :cause)))))
    (is (= [] @store))))

(defn all-keys-present
  [record]
  (= #{:meta-data :fn-args :fn-rv :e :id :tid :c-id :c-tid :c-chain :time :uuid}
     (-> record keys set (disj :fn-value))))

(deftest get-modified-fn-test-set-*modify*
  (testing "fn ran and return return value"
    (let [store (atom [])
          middlewares [(partial middleware store)]
          new-fn (core/get-modified-fn middlewares #'foo)]

      (is (= [1 2] (binding [core/*modify* true]
                     (new-fn [1 2]))))
      (is (= 1 (count @store)))
      (is (true? (all-keys-present (first @store))))))

  (testing "fn rain and failed. Data was captured successfully and exception was thrown"
    (let [store (atom [])
          middlewares [(partial middleware store)]
          new-fn (core/get-modified-fn middlewares #'foo-error)]

      (is (= "Divide by zero" (try (binding [core/*modify* true]
                                     (new-fn 1))
                                   (catch ArithmeticException e
                                     (-> e Throwable->map :cause)))))
      (is (= 1 (count @store)))
      (is (true? (all-keys-present (first @store)))))))

(deftest get-modified-fn-test-set-modify
  (let [store (atom [])
        middlewares [(partial middleware store)]
        new-fn (core/get-modified-fn middlewares #'foo)]
    (reset! core/modify true)

    (is (= [1 2] (new-fn [1 2])))
    (is (= 1 (count @store)))
    (is (true? (all-keys-present (first @store))))

    ; keep it
    (reset! core/modify false)))

(deftest middlewares-test
  (testing "2 different middlewares"
    (let [store-1 (atom [])
          store-2 (atom [])
          new-fn (core/get-modified-fn [(partial middleware store-1)
                                        (partial middleware store-2)] #'foo)]
      (is (= [1 2] (binding [core/*modify* true]
                     (new-fn [1 2]))))
      (is (= 1 (count @store-1)))
      (is (= 1 (count @store-2)))
      (is (true? (all-keys-present (first @store-1))))
      (is (true? (all-keys-present (first @store-2)))))))

; with-modify-fns test -----------------------------------------
(defn boo [x] (foo (str x)))
(defn ooo [x] (boo (inc x)))

(defn get-fs-data
  [store sym]
  (first (filter #(= sym (get-in % [:meta-data :name])) store)))

(deftest with-modify-fns-test
  (let [store (atom [])
        middlewares [(partial middleware store)]
        tracked-vars #{#'foo #'ooo}]
    (is (= "2" (core/with-modify-fns tracked-vars #(ooo 1) middlewares)))
    (is (= 2 (count @store)))
    (is (every? all-keys-present @store))

    (let [ooo-data (get-fs-data @store 'ooo)
          foo-data (get-fs-data @store 'foo)]

      (is (not= nil ooo-data))
      (is (not= nil foo-data))

      ; relations
      (is (= (:uuid foo-data) (:uuid ooo-data)))
      (is (= (:c-id foo-data) (:id ooo-data)))
      (is (= (:tid foo-data) (:tid ooo-data)))
      (is (= (dec (:id foo-data)) (:id ooo-data)))
      (is (= (:c-chain foo-data) [(:id ooo-data)]))
      (is (<= (:time foo-data) (:time ooo-data)))

      ; foo
      (is (= (:e foo-data) nil))
      (is (= (:fn-rv foo-data) "2"))
      (is (= (:fn-args foo-data) '("2")))

      ; ooo
      (is (= (:e ooo-data) nil))
      (is (= (:fn-rv ooo-data) "2"))
      (is (= (:fn-args ooo-data) '(1)))

      (is (= (:c-chain ooo-data) []))
      (is (= (:c-id ooo-data) nil))
      (is (= (:c-tid ooo-data) nil)))))

; TODO: repeat above test but with functions running in different threads.

(deftest restore-altered-fns-test
  (let [tracked-var #'foo
        original-value @#'foo]
    (core/alter-fns #{tracked-var} [])
    (is (contains? (meta #'foo) :i-original-value))
    (is (not= original-value @#'foo))

    (core/restore-altered-fns #{tracked-var})
    (is (false? (contains? (meta #'foo) :i-original-value)))
    (is (= original-value @#'foo))))

(deftest alter-fns-test
  (let [store (atom [])
        middlewares [(partial middleware store)]
        tracked-vars #{#'foo #'ooo}]

    (core/alter-fns tracked-vars middlewares)
    (reset! core/modify true)
    (is (= "2" (ooo 1)))

    (is (= 2 (count @store)))
    (is (every? all-keys-present @store))

    (let [ooo-data (get-fs-data @store 'ooo)
          foo-data (get-fs-data @store 'foo)]

      (is (not= nil ooo-data))
      (is (not= nil foo-data))

      ; relations
      (is (= (:uuid foo-data) (:uuid ooo-data)))
      (is (= (:c-id foo-data) (:id ooo-data)))
      (is (= (:tid foo-data) (:tid ooo-data)))
      (is (= (dec (:id foo-data)) (:id ooo-data)))
      (is (= (:c-chain foo-data) [(:id ooo-data)]))
      (is (<= (:time foo-data) (:time ooo-data)))

      ; foo
      (is (= (:e foo-data) nil))
      (is (= (:fn-rv foo-data) "2"))
      (is (= (:fn-args foo-data) '("2")))

      ; ooo
      (is (= (:e ooo-data) nil))
      (is (= (:fn-rv ooo-data) "2"))
      (is (= (:fn-args ooo-data) '(1)))

      (is (= (:c-chain ooo-data) []))
      (is (= (:c-id ooo-data) nil))
      (is (= (:c-tid ooo-data) nil)))

    (reset! core/modify false)
    (core/restore-altered-fns tracked-vars)))

(defn boo-error [x] (foo-error (inc x)))
(defn ooo-error [x] (boo-error (inc x)))

(deftest with-modify-fns-test-error
  (let [store (atom [])
        middlewares [(partial middleware store)]
        tracked-vars #{#'foo-error #'ooo-error}]
    (is (= "Divide by zero"
           (try
             (core/with-modify-fns tracked-vars #(ooo-error 1) middlewares)
             (catch ArithmeticException e
               (-> e Throwable->map :cause)))))
    (is (= 2 (count @store)))
    (is (every? all-keys-present @store))

    (let [ooo-data (get-fs-data @store 'ooo-error)
          foo-data (get-fs-data @store 'foo-error)]

      (is (not= nil ooo-data))
      (is (not= nil foo-data))

      ; relations
      (is (= (:uuid foo-data) (:uuid ooo-data)))
      (is (= (:c-id foo-data) (:id ooo-data)))
      (is (= (:tid foo-data) (:tid ooo-data)))
      (is (= (dec (:id foo-data)) (:id ooo-data)))
      (is (= (:c-chain foo-data) [(:id ooo-data)]))
      (is (<= (:time foo-data) (:time ooo-data)))

      ; foo
      (is (= (-> foo-data :e Throwable->map :cause) "Divide by zero"))
      (is (= (:fn-rv foo-data) nil))
      (is (= (:fn-args foo-data) '(3)))

      ; ooo
      (is (= (-> ooo-data :e Throwable->map :cause) "Divide by zero"))
      (is (= (:fn-rv ooo-data) nil))
      (is (= (:fn-args ooo-data) '(1)))

      (is (= (:c-chain ooo-data) []))
      (is (= (:c-id ooo-data) nil))
      (is (= (:c-tid ooo-data) nil)))))



(comment
  (defn simplest [i] i)
  (defn simple [i] (simplest i))
  (defn parallel [_] (vec (pmap simple (range 2))))


  (defn simplest-fail [i] (/ i 0))
  (defn simple-fail [i] (simplest-fail i))
  (defn parallel-fail [_] (vec (pmap simple-fail (range 2)))))
