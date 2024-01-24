# inspector

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.akshay/inspector.svg?include_prereleases)](https://clojars.org/org.clojars.akshay/inspector)

See what your functions are doing.

## Install
### Leiningen/Boot
```clojure
[org.clojars.akshay/inspector "1.0.0-SNAPSHOT"]
```

### Clojure CLI/deps.edn
```clojure
org.clojars.akshay/inspector {:mvn/version "1.0.0-SNAPSHOT"}
```

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

### Visualizing all function calls
```clojure
; visualizing all the function being called by (my-fn arg1 arg2 argn)
; print to std-out
(i/print-captured-data my-vars #(my-fn arg1 arg2 argn))

; write to a file instead
(i/spit-captured-data "/tmp/hierarchy.log" my-vars #(my-fn arg1 arg2 argn))
```
#### Example output 
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

### Show calls to database
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

#### Example output 
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

### Get raw data
```clojure
(require '[inspector.capture :as capture])

; rv is return value of (my-fn arg1 arg2 argn)
(let [{:keys [rv fn-call-records]} (capture/run my-vars #(my-fn arg1 arg2 argn)]
   fn-call-records)
```

#### Example output 
From `inspector.test.capture-test`
```clojure
; fn-call-records
[{:fn-name inspector.test.capture-test/parallel, :fn-args (1), :id 1, :c-id 0, :t-id 34, :caller-thread-id 34}
 {:fn-name inspector.test.capture-test/simple, :fn-args (0), :id 2, :c-id 1, :t-id 36, :caller-thread-id 34}
 {:fn-name inspector.test.capture-test/simplest, :fn-args (0), :id 3, :c-id 2, :t-id 36, :caller-thread-id 36}
 {:fn-name inspector.test.capture-test/simplest, :fn-args (0), :id 3, :c-id 2, :t-id 36, :caller-thread-id 36, :fn-rv 0}
 {:fn-name inspector.test.capture-test/simple, :fn-args (0), :id 2, :c-id 1, :t-id 36, :caller-thread-id 34, :fn-rv 0}
 {:fn-name inspector.test.capture-test/simple, :fn-args (1), :id 4, :c-id 1, :t-id 37, :caller-thread-id 34}
 {:fn-name inspector.test.capture-test/simplest, :fn-args (1), :id 5, :c-id 4, :t-id 37, :caller-thread-id 37}
 {:fn-name inspector.test.capture-test/simplest, :fn-args (1), :id 5, :c-id 4, :t-id 37, :caller-thread-id 37, :fn-rv 1}
 {:fn-name inspector.test.capture-test/simple, :fn-args (1), :id 4, :c-id 1, :t-id 37, :caller-thread-id 34, :fn-rv 1}
 {:fn-name inspector.test.capture-test/parallel, :fn-args (1), :id 1, :c-id 0, :t-id 34, :caller-thread-id 34, :fn-rv [0 1]}]

; :fn-name -> is the namespace qualified name of the function that was called
; :fn-args -> are the list of arguments as recieved by the particular function
; :id -> a unique identifier for each function call.
;        if a function is called twice with exact same arguments the both calls will have different id's assigned to them.
; :c-id -> is the id of the caller function. c-id = 0 is always reserved for the very first caller.
; :t-id -> is the thread id of the thread in which a function is executing.
; :caller-thread-id -> is the thread id of the caller function.
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
