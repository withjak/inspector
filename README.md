# inspector

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.akshay/inspector.svg?include_prereleases)](https://clojars.org/org.clojars.akshay/inspector)

See what your functions are doing.

## Install
Check https://clojars.org/org.clojars.akshay/inspector

## Features
- Export data when a function executes. Exports: 
  - `:fn-name` namespace qualified name
  - `:fn-args` arguments
  - `:id`      uniquely identifies each function call
  - `:tid`     thread id
  - `:c-id`    caller functions id
  - `:c-tid`   caller functions thread id
  - `:c-chain` vector of function calls (ids) which eventually lead to call to current fn
  - `:uuid`
  - `:execution-time` time duration (nano second) taken by function to execute
  - `:e` error
  - `:fn-rv` return value
- Thread safe
- Error handling
- Modes
  - Repl debug
  - Omnipresent debug: capture data for all<sup>*</sup> functions, across all threads, all the time

## Basic Usage

### Setup

```clojure
(require '[inspector.inspector :as i]
         '[inspector.fn-find :as fn-find])

; fn-find/get-vars returns a set of all vars which corresponds to functions
; defined in all namespaces, which matches provided regex #"your-code-base.*"
(def my-vars
   ; Generally you would want to track all functions that you have defined.
   (fn-find/get-vars #"your-code-base.*"))
```

### REPL debug

#### Visualizing all function calls
```clojure
; visualizing all the function being called by (my-fn arg1 arg2 argn)
; print to std-out
(i/print-captured-data my-vars #(my-fn arg1 arg2 argn))

; write to a file instead
(i/spit-captured-data "/tmp/hierarchy.log" my-vars #(my-fn arg1 arg2 argn))
```
##### Example output
From `inspector.test.inspector-test`
```
Time: Tue Jan 23 16:28:30 IST 2024
Г-- inspector.test.inspector-test/parallel (1)
|  Г-- inspector.test.inspector-test/simple (0)
|  |  Г-- inspector.test.inspector-test/simplest (0)
|  |  L-- 0
|  L-- 0
|  Г-- inspector.test.inspector-test/simple (1)
|  |  Г-- inspector.test.inspector-test/simplest (1)
|  |  L-- 1
|  L-- 1
L-- [0 1]

Note:
1 is an argument.
Г-- inspector.test.inspector-test/parallel (1)
.
.
.
L-- [0 1]
[0 1] is thre return value on calling (parallel 1)
```

#### Show calls to database
```clojure
; Calling you function to see if its performing any CRUD operations in mongodb (or any other library/libraries)
(i/print-calls-to-tracked-vars 
   (fn-find/get-vars #"mongodb.*")  ; Track all functions defined in mongodb library
   my-vars
   #(my-fn arg1 arg2 argn))

; you can write to file instead
(i/spit-calls-to-tracked-vars
   (fn-find/get-vars #"mongodb.*") 
   my-vars
   #(my-fn arg1 arg2 argn))
```

##### Example output 
From `inspector.test.inspector-test`
```
Time: Tue Jan 23 17:32:22 IST 2024
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ 
call-chain: ["inspector.test.inspector-test/parallel" "inspector.test.inspector-test/simple" "inspector.test.inspector-test/simplest"]
name: inspector.test.inspector-test/simplest
args: (1)
rv: 1
♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ ♤ ♧ ♡ ♢ 
call-chain: ["inspector.test.inspector-test/parallel" "inspector.test.inspector-test/simple" "inspector.test.inspector-test/simplest"]
name: inspector.test.inspector-test/simplest
args: (0)
rv: 0

Note: 
call-chain: shows the order in which different functions were called which finally resulted in call to a tracked fn.
```

#### Get raw data
```clojure
; rv is return value of (my-fn arg1 arg2 argn)
(let [{:keys [rv fn-call-records]} (i/export-raw my-vars #(my-fn arg1 arg2 argn)]
   fn-call-records)
```

##### Example output 
From `inspector.test.capture-test`
```clojure
; fn-call-records
[{:c-chain []    :id 1 :c-id nil :fn-name "inspector.test.capture-test/parallel" :fn-args (1) :tid 34 :c-tid nil :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e"}
 {:c-chain [1]   :id 2 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (0) :tid 30 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e"}
 {:c-chain [1 2] :id 4 :c-id 2   :fn-name "inspector.test.capture-test/simplest" :fn-args (0) :tid 30 :c-tid 30  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e"}
 {:c-chain [1]   :id 3 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (1) :tid 29 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e"}
 {:c-chain [1 2] :id 4 :c-id 2   :fn-name "inspector.test.capture-test/simplest" :fn-args (0) :tid 30 :c-tid 30  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :execution-time 6584   :fn-rv 0}
 {:c-chain [1 3] :id 5 :c-id 3   :fn-name "inspector.test.capture-test/simplest" :fn-args (1) :tid 29 :c-tid 29  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e"}
 {:c-chain [1]   :id 2 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (0) :tid 30 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :execution-time 49583  :fn-rv 0}
 {:c-chain [1 3] :id 5 :c-id 3   :fn-name "inspector.test.capture-test/simplest" :fn-args (1) :tid 29 :c-tid 29  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :execution-time 1625   :fn-rv 1}
 {:c-chain [1]   :id 3 :c-id 1   :fn-name "inspector.test.capture-test/simple"   :fn-args (1) :tid 29 :c-tid 34  :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :execution-time 42625  :fn-rv 1}
 {:c-chain []    :id 1 :c-id nil :fn-name "inspector.test.capture-test/parallel" :fn-args (1) :tid 34 :c-tid nil :uuid #uuid "4c3bf13a-7899-4202-ade6-cfa0dfc3955e" :execution-time 431833 :fn-rv [0 1]}]

; :fn-rv -> return value. Always check :e when :fn-rv is nil
; :e -> will only be present in case an error was raised. In this case :fn-rv will be set as nil.
; :id -> a unique identifier for each function call.
;        if a function is called twice with exact same arguments the both calls will have different id's assigned to them.
; :c-id -> is the id of the caller function. 
;          c-id = nil implies caller is unknown. 
;          Either because caller function is not modified (bcoz its not part of my-project-vars),
;          Or caller function value if directly being called. Example in case of most handler fns.
; :c-chain -> vector of `:id`. {:id 5 :c-chain [1 2 3 4]} => that :if 5 was called by 4 and 4 was called 3 and so on.
; :uuid -> unique id to identify all the fns (even if some of them ran in different threads) which ran because of call to a top level function.
;          useful when using Omnipresent debug mode.
```

### Ominpresent debug
```clojure
(defn export-fn 
  [{:keys [:fn-name :fn-args :fn-rv :id :tid :c-id :c-tid :c-chain :uuid :execution-time :e] :as data}]
  ; send to elasticsearch
  ; or log it
  )
  
; export-fn will be called every time a function execution completes
(i/stream my-project-vars export-fn)
```

## How inspector work?

In clojure a function's name is a `symbol`.
The `symbol` maps to a `var` which has a reference to `value`.
Think of `value` as the actual function which will run when you do `(function-name arg1 arg2)`.
<img src="./resources/original_function.png">
<br>
<br>
The idea is to change the reference present in `var` to point to a `new value` (or new function).
This `new value` (or new function) will wrap the original `value` (or function) with additional code.
<img src="./resources/modified_function.png">
<br>
<br>
Inspector provides a structured way to modify a lot of `values`(functions) at once in this way.

## Todo
- expose `start-streaming` from `inspector.inspector`
- finally standardise public api
- refactor `capture.clj` to use `stream.clj`
- complete tests
- function to stringify function arguments, such as atom, object, ...
- simplify `inspector.inspector` for printing call hierarchy

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
