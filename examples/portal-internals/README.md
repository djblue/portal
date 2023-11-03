# Portal Internals

---

# Who AM I

- Chris Badahdah
- Software Development for ~9
  - Lots of Java and Javascript
  - Mostly Web Application
- Grew appreciation for Lisp / Scheme via SICP
  - Found Clojure!
  - Immutable by default
- Doing Clojure Professionally for last ~4 years
  - Working on Portal for 3.5 years

---

# Goals

- What is Portal?
- Show how Portal can use used in various workflows
  - Customization at different levels
  - Motivate people to explore new workflows
- Explore Portal's internals
  - Encourage contributions
  - Learn some cool things along the way
- Inspiration for other viz implementations

---

# Portal?

- Data visualization tool
  - Data structure focused
- Great place to send `tap>` values
  - Added in Clojure `1.10`
- Portal is like stdio for `tap>`
  - Focus on data over string
- Web App with Ring server + Reagent client
  - Multi-platform (jvm, node, clr)

---

# Default `tap>` Workflow

```clojure
(require '[portal.api :as p])
(p/open) ;; Open a new inspector
```

```clojure
(add-tap #'p/submit) ;; Add portal as a tap> target
```

```clojure
(tap> :hello/world) ;; Start tapping out values
(tap> (read-string (slurp "deps.edn")))
```

```clojure
(remove-tap #'p/submit) ;; Remove portal from tap> targetset
```

### Pros

- Very easy to get going
- Reasonable default behavior

### Cons

- Accumulates all values until clear
- Not particularly suited for any particular workflow
- Subject to `tap>` limitations

```clojure
(dotimes [_ 100000] (tap> :hello/world))
```

---

# Default viewer Part 1

How do I configure default viewers?

### Challenges

- How can a user specify this mapping?
  - pure data?
  - code?
    - where does the code run?
    - user's runtime?

### Portal's approach

- Start with the information model
- `:portal.viewer/default` in metadata
  - Not every value supports metadata
  - `portal.viewer` makes this easier

---

# Default viewer Part 2

- Leverage custom submit function
  - Arbitrary dispatch
  - Arbitrary transformations

```clojure
(require '[portal.viewer :as v])

(def defaults
  {string? v/text
   bytes?  v/bin})

(defn- get-viewer-f [value]
  (or (some (fn [[predicate viewer]]
              (when (predicate value)
                viewer))
            defaults)
      v/tree))
```

```clojure
(require '[portal.api :as p])
(p/open)

(defn submit [value]
  (let [f (get-viewer-f value)]
    (p/submit (f value))))

(add-tap #'submit)
```

```clojure
(tap> "hello, world")
(tap> (byte-array [0 1 2 3]))
(tap> (range 10))
```

- Not obvious but very powerful
- Can be tricky for those unfamiliar with metadata

---

# Datafy 

How do I turn on clojure.datafy/datafy by default?

- Also added in Clojure 1.10
- Protocol for turning objects into data

```clojure
(require '[clojure.datafy :as d])
(require '[portal.api :as p])

(p/open)
(def submit (comp p/submit d/datafy))
(add-tap #'submit)
```

```clojure
(tap> (find-ns 'clojure.core))
(tap> java.lang.Long)
```

- Opt-in instead of opt-out

---

# Async Values

Printing / tapping promises by default isn't very useful


```clojure
(require '[portal.api :as p])
(p/open)

(defn promise? [x]
  (and
   (instance? clojure.lang.IDeref x)
   (instance? clojure.lang.IPending x)))

(defn async-submit [value]
  (p/submit
   (if-not (promise? value)
     value
     (deref value 5000 :timeout))))

(add-tap #'async-submit)
```

```clojure
(def p (promise))
(tap> p)
```

```clojure
(deliver p :delivered)
```

- Equally applicable to JavaScript promises
  - Cognitive load during debugging
  - Beats `#object [Promise ...]`

---

# `tap>` via Tooling

Using `portal.nrepl`, we can capture more context:

- Capture more context
  - Source / Runtime info
  - Timing
  - stdio
  - Test assertion
  - Exceptions

```clojure
(require '[portal.api :as p])
(require '[clojure.java.io :as io])
(binding [*default-data-reader-fn* tagged-literal]
  (p/inspect (read-string (slurp (io/resource "nrepl-log.edn")))))
```

- Still intercept + process in custom submit function
- One of the main ways I use Portal

---

# `p/submit` Explored

```clojure
(def tap-list (atom []))

(defn submit [value]
  (swap! tap-list conj value))

(add-tap #'submit)
```

```clojure
(tap> :world)
```

```clojure
(require '[portal.api :as p])
(p/inspect tap-list)
```

```clojure
(tap> :hello)
```

- Portal actually only cares about data + atoms
- You can `p/inspect` multiple values simultaneously

```clojure
(p/inspect (read-string (slurp "bb.edn")))
```

---

# Inspecting Values

```clojure
(require '[portal.api :as p])
(p/open {:mode :dev :debug :server})
```

- Connections
- Value Cache
  - For capturing objects / collections
  - Allows for compression
- Watch Registry
  - Tracks what atoms are watched for session

```clojure
(p/submit :hello)
(p/submit :world)
```

```clojure
(def a (atom 0))
(p/submit a)
```

```clojure
(swap! a inc)
```

---

# Watching Atoms

- When serializing values, Portal will `add-watch` any atoms
  - No platform specific abstractions
- Clojure's state model makes Portal's implementation much simpler

```clojure
(defn- atom? [o]
  #?(:clj  (instance? clojure.lang.Atom o)
     :cljr (instance? clojure.lang.Atom o)
     :cljs (satisfies? cljs.core/IAtom o)))
```

```clojure
(defn- set-timeout [f ^long timeout]
  #?(:clj  (future (Thread/sleep timeout) (f))
     :cljr (future (System.Threading.Thread/Sleep timeout) (f))
     :cljs (js/setTimeout f timeout)))

(defn- invalidate [session-id a old new]
  (when-not (= old new)
    (set-timeout
     #(when (= @a new)
        (when-let [request @request]
          (request session-id {:op :portal.rpc/invalidate :atom a})))
     100)))
```

- Changing metadata on a value will not cause a notification
  - `=` ignore metadata
- Portal eagerly serializes all values
  - Except atoms

---

# Portal Client

```clojure
(require '[portal.api :as p])
(p/open {:mode :dev :debug :client})
```

```clojure
(p/submit :hello)
(p/submit :world)
```

- Value Context
  - Where is a value relative to other values
- RPC
- Selection Index
  - Indexing of relative values

---

# portal-test-reporter

```clojure
(require '[clojure.test :refer [deftest is]])
(require '[dev.freeformsoftware.portal-test-runner :refer [run-test]])

(deftest example-pass (is (= 1 1)))
(deftest example-fail (is (= 1 2)))

(run-test {} *ns*)
```

```clojure
;; for re-open
(reset! dev.freeformsoftware.portal-test-runner/portal-instance nil)
```

- Customization via hiccup viewer
  - Easy to move the full reagent viewer

[https://github.com/JJ-Atkinson/portal-test-reporter](https://github.com/JJ-Atkinson/portal-test-reporter)

---

# Playback

- Porting existing tools to use Portal

```clojure
(require '[playback.preload])
```

```clojure
#>>(defn factorial [n]
     (if (zero? n)
       1
       (* n (factorial (dec n)))))
```

```clojure
(factorial 10)
```

[https://github.com/gnl/playback](https://github.com/gnl/playback)

---

# Emmy Viewers

```clojure
(require '[emmy.portal :refer [start!]])

(start!
 {:emmy.portal/tex
  {:macros {"\\f" "#1f(#2)"}}
  :theme :portal.colors/zenburn})  
```

```clojure
(require '[emmy.mathlive :as ml])
(tap> (ml/mathfield {:default-value "1+x"}))
```

```clojure
(require '[emmy.env :as e :refer :all])
(require '[emmy.mafs :as mafs])
(require '[emmy.viewer :as ev])
(tap>
 (mafs/mafs-meta
  (ev/with-let [!phase [0 0]]
    (let [shifted (ev/with-params {:atom !phase :params [0]}
                    (fn [shift]
                      (((cube D) tanh) (- identity shift))))]
      (mafs/mafs
       (mafs/cartesian)
       (mafs/of-x shifted)
       (mafs/inequality
        {:y {:<= shifted :> cos} :color :blue})
       (mafs/movable-point {:atom !phase :constrain "horizontal"}))))))
```

- Extensive use of npm dependencies
  - Targets Reagent via ClojureScript eval
- Initially designed for other tools

[https://github.com/mentat-collective/emmy-viewers](https://github.com/mentat-collective/emmy-viewers)

---

# Portal Build

```clojure
(require '[portal.api :as p])
(require '[tasks.check :as check])
(require '[tasks.test :as test])
(require '[tasks.parallel :refer [*portal* with-out-data]])
```

```clojure
(binding [*portal* false]
  (p/inspect (with-out-data (check/check) (test/test) (test/cljr))))
```

- Augment output with data

```clojure
(p/inspect (with-out-data (check/check) (test/test) (test/cljr)))
```

- Parallel build?

```clojure
(p/inspect (with-out-data (check/check*) (test/test*)))
```

- Re-use viewers in different contexts

---

# Benchmarking

- Portal use custom serialization format
  - CSON
  - Mostly like transit
- Why Custom?
  - Transitive deps
  - Implemented as cljc
  - Specific optimization for Portal
- How is the performance?

```clojure
(require '[portal.api :as p])
(p/open)
(add-tap p/submit)
```

```clojure
(require '[portal.runtime.bench-cson :as cson])

(def data (cson/run-benchmark))
(tap> :done)
```

```clojure
(tap> (cson/charts data))
(tap> (cson/table data))
(tap> data)
```

---

# UI Extensibility

```clojure
(require '[portal.api :as p])
(p/open {:main 'tetris.core/app})
```

- One of my first CLJS projects
- SCI (Small Clojure Interpreter)
  - Fast enough
  - Supports large subset of ClojureScript

[https://github.com/djblue/tetris](https://github.com/djblue/tetris)

---

# Documentation

```clojure
(require '[portal.api :as p])
(p/docs)
```

- Augments static docs
- Could become a generic facility for interactive docs
- Contributing guides is always appreciated!

---

# Current Limitations

- No smart serialization around infinite values
  - No streaming for serialization
- Some types in ClojureScript are wonky
  - Longs
  - Dates
- Lazy values can break internals
- Client will track values via `WeakRef`
  - can evicted from `:value-cache` via `FinalizationRegistry`
  - not perfect

---

# Future Goals

- Simplify extensibility
  - Ease packaging and distribution
- Explore / enable /document more workflow
- User defined keyboard shortcuts

---

# Conclusion

- Everyone works differently
- Portal aims to fit into many workflows
  - Discoverability is an issue
- Customization comes in many forms
  - To get the most out of portal, some customization is required
- The more you datafy your workflow, the more your can get out of Portal

---

# Thanks!

<div style="display: flex; align-items: center; height: 400px">
<ul style="padding: 40px; font-size: 1.8em">
<li><a href="https://github.com/djblue/portal">https://github.com/djblue/portal</a></li>
<li>
<a href="https://clojurians.slack.com/channels/portal">#portal</a> on clojurians slack
</li>
<li>
Github Sponsors
<a href="https://github.com/sponsors/djblue">https://github.com/sponsors/djblue</a>
</li>
<ul>
</div>