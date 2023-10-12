(defproject org.clojars.akshay/inspector "0.4.0-SNAPSHOT"
  :description "See when your functions are called. See function call hierarchy, db data used when a particular function is called, and export data"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [cheshire "5.11.0"]]
  :repl-options {:init-ns inspector.core})
