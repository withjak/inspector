(defproject org.clojars.akshay/inspector "1.1.2"
  :description "See when your functions are called. See function call hierarchy, db data used when a particular function is called, and export data"
  :url "https://github.com/withjak/inspector"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org/"
                                    :creds :gpg}]]
  :repl-options {:init-ns inspector.core})