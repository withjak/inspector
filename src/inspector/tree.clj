(ns inspector.tree)

(defn flatten-tree
  "Returns a depth-first traversal of the tree. Where :start and :end represents
  the start or end of a node's exploration.

  adjacency-list: map => { parent-node  [child child ...], ... }
  root: root node

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
  node: unique index of each node
  start/end:
    [[node1 :start] ... [node1 :end]]
    everything in between are children of node1
  level: level of node in the tree
  "
  [adjacency-list root]
  (letfn [(flatten-tree
            [node level]
            (let [children (get adjacency-list node)]
              (concat
                [node :start level]
                (mapcat #(flatten-tree % (inc level)) children)
                [node :end level])))]
    (partition 3 (flatten-tree root 0))))

(defn find-path
  "Returns path which is a list of nodes from root node to target node.

  parent-pointer-tree:
    mapping from child node to parent node
    see: https://en.wikipedia.org/wiki/Parent_pointer_tree

  (find-path {1 0
              2 1
              3 1
              4 2
              5 3}
             5)
  => (1 3 5)
  "
  [parent-pointer-tree target-node]
  (loop [node target-node
         path (list target-node)]
    (let [parent (get parent-pointer-tree node)]
      (if (zero? parent)
        path
        (recur parent (conj path parent))))))
