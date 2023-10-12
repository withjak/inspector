(ns inspector.utils)

(defn update-vals
  "Apply f on each value of map m."
  [m f]
  (zipmap (keys m)
          (map f (vals m))))
