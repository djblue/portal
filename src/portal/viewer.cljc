(ns portal.viewer
  "Namespace for easily setting default viewers for provided values.

   Note: Support for input validation may come in the future."
  (:refer-clojure :exclude [pr-str for]))

(defn default
  ([value viewer]
   (vary-meta value assoc ::default viewer))
  ([value viewer opts]
   (vary-meta value assoc ::default viewer viewer opts)))

(defn for [value opts]
  (vary-meta value update ::for merge opts))

(defn ex
  "Viewer for datafied exceptions."
  ([value] (default value ::ex))
  ([value opts] (default value ::ex opts)))

(defn log
  "Useful for conveying a value in a specific context (what/where/when)."
  ([value] (default value ::log))
  ([value opts] (default value ::log opts)))

(defn http
  "Highlight HTTP method and status code for http request and response."
  ([value] (default value ::http))
  ([value opts] (default value ::http opts)))

(defn test-report
  "View clojure.test report output."
  ([value] (default value ::test-report))
  ([value opts] (default value ::test-report opts)))

(defn color
  "View hex / rgb / rgba colors"
  ([value] (default value ::color))
  ([value opts] (default value ::color opts)))

(defn duration-ns
  "Interpret number as a duration in nanoseconds, round up to minutes."
  ([value] (default value ::duration-ns))
  ([value opts] (default value ::duration-ns opts)))

(defn duration-ms
  "Interpret number as a duration in milliseconds, round up to minutes."
  ([value] (default value ::duration-ms))
  ([value opts] (default value ::duration-ms opts)))

(defn pprint
  "View value printed via clojure.pprint/pprint."
  ([value] (default value ::pprint))
  ([value opts] (default value ::pprint opts)))

(defn vega-lite
  ([value] (default value ::vega-lite))
  ([value opts] (default value ::vega-lite opts)))

(defn vega
  ([value] (default value ::vega))
  ([value opts] (default value ::vega opts)))

(defn table
  "View value as a table. Supports sticky headers and keyboard navigation.
   opts:
   - columns: vector of keys to use as columns in table."
  ([value] (default value ::table))
  ([value opts] (default value ::table opts)))

(defn tree
  "For viewing highly nested values, such as hiccup."
  ([value] (default value ::tree))
  ([value opts] (default value ::tree opts)))

(defn pr-str
  ([value] (default value ::pr-str))
  ([value opts] (default value ::pr-str opts)))

(defn hiccup
  "Render a hiccup value as html via reagent."
  ([value] (default value ::hiccup))
  ([value opts] (default value ::hiccup opts)))

(defn date-time
  ([value] (default value ::date-time))
  ([value opts] (default value ::date-time opts)))

(defn diff
  "Diff a collection of values successively starting with the first two."
  ([value] (default value ::diff))
  ([value opts] (default value ::diff opts)))

(defn prepl
  "View interlacing of stdout, stderr and tap values. Useful for build output."
  ([value] (default value ::prepl))
  ([value opts] (default value ::prepl opts)))