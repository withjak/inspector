(ns inspector.templates.cross_group
  (:require [clojure.pprint :as pprint]
            [clojure.string :refer [join]]
            [inspector.core :as core]
            [inspector.templates.common :as common]))

(def path (atom []))

(defn ^:before-condition cross-group-call?
  "When current function, and it's caller function
  are of different groups return true"
  [member-group-map meta-data & _]
  (let [called (keyword (common/full-name meta-data))
        caller (or (keyword (peek @path)) called)]
    (not= (get member-group-map called)
          (get member-group-map caller))))

(defn ^:action before-print
  [meta-data fn-args _]
  (println (common/color-font "Function: " :CYAN)
           (common/color-bg (common/full-name meta-data) :CYAN))
  (println (common/color-font "Path: " :CYAN)
           @path)
  (println (common/color-font "Fn-args: " :CYAN))
  (pprint/pprint fn-args))

(defn ^:action after-print
  [meta-data fn-args _ return-value]
  (println (common/color-font "Return-value: " :CYAN))
  (pprint/pprint return-value)
  (println (common/color-font (join (repeat 20 "♤ ♧ ♡ ♢ ")) :BLACK)))

(defn ^:before-action append-to-path
  [meta-data & _]
  (swap! path conj (common/full-name meta-data)))

(defn ^:after-action pop-from-path
  [& _]
  (swap! path pop))

(defn cross-group-call
  [group-n-members-map]
  (let [full-name-keyword #(keyword (common/full-name (meta %)))
        reverse-k-v-map (fn [[k v]]
                          (zipmap (map full-name-keyword v) (repeat k)))
        member-group-map (into {} (map reverse-k-v-map group-n-members-map))]
    (core/create-template [(partial cross-group-call? member-group-map) before-print ;; print if caller and called functioned are from different groups
                           common/always append-to-path]    ;; always append current function to 'path'
                          [common/always pop-from-path      ;; fn has returned, so remove it from path
                           (partial cross-group-call? member-group-map) after-print]))) ;; print if caller and called functioned are from different groups
