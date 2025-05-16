(ns portal.viewer
  "Namespace for easily setting default viewers for provided values.

   Note: Support for input validation may come in the future."
  (:refer-clojure :exclude [pr-str for]))

(declare hiccup)

(defn- can-meta? [value]
  #?(:clj  (instance? clojure.lang.IObj value)
     :cljr (instance? clojure.lang.IObj value)
     :joyride
     (try (with-meta value {}) true
          (catch :default _e false))
     :org.babashka/nbb
     (try (with-meta value {}) true
          (catch :default _e false))
     :cljs (implements? IMeta value)
     :lpy
     (try (with-meta value {}) true
          (catch Exception _e false))))

(defn default
  "Set the default viewer for a value.

  Note: The return for values that don't support metadata may change in the
        future."
  ([value viewer]
   (if (can-meta? value)
     (vary-meta value assoc ::default viewer)
     (hiccup [::inspector {::default viewer} value])))
  ([value viewer opts]
   (if (can-meta? value)
     (vary-meta value assoc ::default viewer viewer opts)
     (hiccup [::inspector {::default viewer viewer opts} value]))))

(defn for [value opts]
  (vary-meta value update ::for merge opts))

(defn inspector
  "Default data viewer."
  ([value] (default value ::inspector))
  ([value opts] (default value ::inspector opts)))

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

(defn diff-text
  "Diff two strings."
  ([value] (default value ::diff-text))
  ([value opts] (default value ::diff-text opts)))

(defn prepl
  "View interlacing of stdout, stderr and tap values. Useful for build output."
  ([value] (default value ::prepl))
  ([value opts] (default value ::prepl opts)))

(defn markdown
  "Parse string as markdown and view as html."
  ([value] (default value ::markdown))
  ([value opts] (default value ::markdown opts)))

(defn bin
  "View binary data as a hexdump."
  ([value] (default value ::bin))
  ([value opts] (default value ::bin opts)))

(defn image
  "View a binary value as an image."
  ([value] (default value ::image))
  ([value opts] (default value ::image opts)))

(defn text
  "View string as a text file."
  ([value] (default value ::text))
  ([value opts] (default value ::text opts)))

(defn json
  "Parse a string as JSON. Will render error if parsing fails."
  ([value] (default value ::json))
  ([value opts] (default value ::json opts)))

(defn jwt
  "Parse a string as a JWT. Will render error if parsing fails."
  ([value] (default value ::jwt))
  ([value opts] (default value ::jwt opts)))

(defn edn
  "Parse a string as EDN. Will render error if parsing fails."
  ([value] (default value ::edn))
  ([value opts] (default value ::edn opts)))

(defn transit
  "Parse a string as transit. Will render error if parsing fails."
  ([value] (default value ::transit))
  ([value opts] (default value ::transit opts)))

(defn csv
  "Parse a string as a CSV and use the table viewer by default."
  ([value] (default value ::csv))
  ([value opts] (default value ::csv opts)))

(defn html
  ([value] (default value ::html))
  ([value opts] (default value ::html opts)))

(defn code
  ([value] (default value ::code))
  ([value opts] (default value  ::code opts)))

(defn size-bytes
  ([value] (default value ::size-bytes))
  ([value opts] (default value  ::size-bytes opts)))