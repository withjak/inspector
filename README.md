# inspector

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.akshay/inspector.svg?include_prereleases)](https://clojars.org/org.clojars.akshay/inspector)

**Inspector** is a tool for profiling, debugging, tracing, and visualizing function call hierarchies in Clojure applications. It provides insights into who is calling whom, with what arguments, what was returned, execution time, and more.

# Table of Contents
- [Add dependency](#Add-dependency)
- [Features](#Features)
- [Basic Usage](#Basic-Usage)
  - [Setup](#Setup)
  - [Normal mode](#Normal-mode)
  - [Omnipresent mode](#Omnipresent-mode)
  - [Important Notes](#Important-Notes)
- [Detailed Usage](#Detailed-Usage)
  - [Normal Mode: Output](#Normal-Mode-Output)
  - [Normal Mode: Raw Data](#Normal-Mode-Raw-Data)
  - [Omnipresent Mode: REPL](#Omnipresent-Mode-REPL)
- [Tracking Specific Functions or Namespaces](#Tracking-Specific-Functions-or-Namespaces)
- [Skipping Function Tracking](#Skipping-Function-Tracking)
- [Middleware](#Middleware)

## Add dependency
Add the following dependency to your project:
### Leiningen
```clojure
[org.clojars.akshay/inspector "1.1.3-SNAPSHOT"]
```

### Clojure CLI/deps.edn
```clojure
org.clojars.akshay/inspector {:mvn/version "1.1.3-SNAPSHOT"}
```

## Features
- **Minimal API**: `get-vars`, `iprint`, `ispit`, `stream-raw`.
- **Fine-grained control**: Track specific functions and namespaces.
- **Low performance overhead**.
- **Multiple Modes**:
  - **Normal Mode**: Get human-readable output for specific function calls. (`iprint`, `ispit`)
  - **Omnipresent Mode**: Continuously capture function calls across all threads. (`stream-raw`)
- **Middleware**: Inject custom code before and after tracked functions executions.
- **Detailed Insights for Each Function Call**:
  - `:fn-name`: Namespace-qualified function name.
  - `:time`:    Execution time (in nanoseconds).
  - `:fn-args`: Arguments passed.
  - `:fn-rv`:   Return value.
  - `:e`:       Errors (if any).
  - `:id`:      Unique ID for the function call.
  - `:tid`:     Thread ID.
  - `:c-id`:    Caller’s ID.
  - `:c-tid`:   Caller’s thread ID.
  - `:c-chain`: Call chain (vector of function ids).
  - `:uuid`:    All function calls resulting from a top-level function invocation have same uuid.


## Basic Usage

### Setup
Start by requiring the necessary namespace:
```clojure
(require '[inspector.inspector :as i])
```
Next, define the functions you want to track using get-vars:
```clojure
(def tracked-vars (i/get-vars #"project-prefix.*"))
```

### Normal mode
To print function calls in a readable format, use:
```clojure
(i/iprint tracked-vars #(my-fn arg1 arg2 argn))
```
Or, write the output to a file:
```clojure
(i/ispit "/tmp/hierarchy.log" tracked-vars #(my-fn arg1 arg2 argn))
```
Example output from `inspector.test.inspector-test`:
```roomsql
Time: Tue Jan 23 16:28:30 IST 2024
Г-- inspector.test.inspector-test/parallel (1) <-- arguments
|  Г-- inspector.test.inspector-test/simple (0)
|  |  Г-- inspector.test.inspector-test/simplest (0)
|  |  L-- 0
|  L-- 0
|  Г-- inspector.test.inspector-test/simple (1)
|  |  Г-- inspector.test.inspector-test/simplest (1)
|  |  L-- 1
|  L-- 1
L-- [0 1] <-- return value
```

### Omnipresent mode
To capture data continuously:
```clojure
(defn export 
  [{:keys [:fn-name :fn-args :fn-rv :e :time :id :tid :c-id :c-tid :c-chain :uuid]} :as record]
  ;; Handle the captured data (e.g., log it, send to a database, etc.)
  (clojure.tools.logging/info (dissoc record :fn-args :fn-rv)))
  
;; export will be called every time a function execution completes
;; place it somewhere near the top of -main function
(i/stream-raw tracked-vars export)
```

### Important Notes
- **Normal Mode** (`iprint`, `ispit`): Use for targeted debugging of specific top level function.
- **Omnipresent Mode** (`stream-raw`): Use for continuous data collection. When running **via repl** in a remote environment (staging/production), restore the environment as described in [Omnipresent Mode: REPL](#Omnipresent-Mode-REPL).


## Detailed Usage
### Normal Mode: Output
Customize the output of `iprint` and `ispit` using options.
```clojure 
(i/iprint tracked-vars #(my-fn arg1 arg2) {:start [:time :fn-args]})
```
Output:
```roomsql
Г-- fn-name time fn-args
|  Г-- fn-name time fn-args
|  |  Г-- fn-name time fn-args
|  |  L-- fn-rv
|  L-- fn-rv
|  Г-- fn-name time fn-args
|  L-- fn-rv
L-- fn-rv
```

Another example
```clojure 
(i/iprint tracked-vars #(my-fn arg1 arg2) {:expanded-view? false 
                                           :start [:time :fn-rv]})
```
Output:
```roomsql
--> fn-name time fn-rv
   --> fn-name time fn-rv
      --> fn-name time fn-rv
   --> fn-name time fn-rv
```
You can further tweak the output by providing different options to control indentation, markers, and more.
Check `i/parse-opts` to see all possible options.

### Normal Mode: Raw Data
Get raw data for advanced processing:
```clojure
; rv is return value of (my-fn arg1 arg2 argn)
(let [{:keys [e rv records]} (i/export-raw tracked-vars #(my-fn arg1 arg2 argn)] 
  records)
```

Example output from `inspector.test.capture-test`:
```clojure
[{:c-chain [1 2] :id 4 :c-id 2   :fn-name "inspector.test.capture-test/simplest" :fn-args (0) :tid 30 :c-tid 30  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :time 6584   :fn-rv 0}
 {:c-chain [1]   :id 2 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (0) :tid 30 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :time 49583  :fn-rv 0}
 {:c-chain [1 3] :id 5 :c-id 3   :fn-name "inspector.test.capture-test/simplest" :fn-args (1) :tid 29 :c-tid 29  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :time 1625   :fn-rv 1}
 {:c-chain [1]   :id 3 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (1) :tid 29 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :time 42625  :fn-rv 1}
 {:c-chain []    :id 1 :c-id nil :fn-name "inspector.test.capture-test/parallel" :fn-args (1) :tid 34 :c-tid nil :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :time 431833 :fn-rv [0 1]}]
```

### Omnipresent Mode: REPL
If you're tracking function calls in a remote environment via REPL by using `stream-raw`, make sure to restore the original function definitions once done:
```clojure
(inspector.track/un-track tracked-vars)
```

## Tracking Specific Functions or Namespaces
Use `get-vars` (which returns a set) to collect vars from specific namespaces. Then pass them to `iprint`, `ispit`, or `stream-raw` to start tracking them.

```clojure
(i/get-vars #"project-prefix.*")                ; set of all functions from all namespaces.

(i/get-vars #"project-prefix.c")                ; set of all functions from project-prefix.c namespace

(clojure.set/difference                         ; set of all functions except those defined in project-prefix.c namespace
  (i/get-vars #"project-prefix.*")  
  (i/get-vars #"project-prefix.c"))

(set/difference                                 ; set of all functions except function project-prefix.c/c-2
  (i/get-vars #"project-prefix.*") 
  #{#'dummy.c/c-2})
```
**Note**: <br>
If the function call sequence is `a -> b -> c` and only `a` and `c` are being tracked, you'll still receive information showing `a -> c`.

## Skipping Function Tracking
To skip tracking a specific function, you can either remove its var from tracked-vars or add :`i-skip` metadata:
```clojure
(defn ^:i-skip foo
  [args]
  ...)
```

## Middleware
You can use middleware to run custom code before and after the execution of every tracked function.

```clojure 
(defn nano->ms-middleware
  "Converts execution time from nanoseconds to milliseconds."
  [handler]
  (fn [{:keys [fn-args fn-meta fn-rv e time id tid c-id c-tid c-chain uuid] :as state}]
    (let [new-state (handler state)]
      (update new-state :time nano->ms))))
```
To wrap tracked functions with your custom middleware, check out:
- `stream-raw` : for omnipresent mode.
- `export-raw` : for normal mode.

## License

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
