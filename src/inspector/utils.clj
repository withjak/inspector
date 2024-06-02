(ns inspector.utils)

(def ^:const reset-color "\u001B[0m")

(def ^:const font-colors
  {:BLACK  "\u001B[30m"
   :RED    "\u001B[31m"
   :GREEN  "\u001B[32m"
   :YELLOW "\u001B[33m"
   :BLUE   "\u001B[34m"
   :PURPLE "\u001B[35m"
   :CYAN   "\u001B[36m"
   :WHITE  "\u001B[37m"})

(def ^:const bg-colors
  {;:BLACK  "\u001B[40m"
   :RED    "\u001B[41m"
   :GREEN  "\u001B[42m"
   :YELLOW "\u001B[43m"
   :BLUE   "\u001B[44m"
   :PURPLE "\u001B[45m"
   :CYAN   "\u001B[46m"
   ;:WHITE  "\u001B[47m"
   })

(defn color-font
  [string color]
  (str (get font-colors color) string reset-color))

(defn color-bg
  [string color]
  (str (get bg-colors color) string reset-color))

(defn full-name
  [meta-data]
  (str (:ns meta-data) "/" (:name meta-data)))

(defn ^:condition always
  [& _]
  true)

(defn prepare-fn-record
  [meta-data fn-args {:keys [caller-thread-id t-id c-id id execution-time e]}]
  (merge
    {:fn-name          (full-name meta-data)
     :fn-args          fn-args
     :id               id
     :c-id             c-id
     :t-id             t-id
     :caller-thread-id caller-thread-id}
    (when execution-time {:execution-time execution-time})
    (when e {:e (str e)})))

