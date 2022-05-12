# Custom Tap List

If the default behavior of Portal's tap list isn't exactly what you need, you
can implement your own with any behavior your require.

## Constraints

For this example, let's assume you have the following constraints:

- The tap list should be displayed as a table
- Every tapped value should have an `:id` and a `:time`
- The order of columns should be `:id`, `:value`, and `:time`
- The tap list should grow at the bottom (chronological)
- The tap list should not exceed 25 items, dropping elements from the top

## Implementation

Firstly, lets specify metadata to tell Portal how to render the tap-list:

```clojure
(def viewer
  {:portal.viewer/default :portal.viewer/table
   :portal.viewer/table {:columns [:id :value :time]}})

(def tap-list (atom (with-meta [] viewer)))
```

Secondly, setup a custom submit function to implement the stated constraints:

```clojure
(def ids (atom 0))

(defn submit [value]
  (let [id (swap! ids inc)]
    (swap! tap-list
           (fn [taps]
             (conj
              (if (< (count taps) 25)
                taps
                (subvec taps 1))
              {:id    id
               :value value
               :time  (java.util.Date.)})))))
```

Finally, you can wire all this up into Portal with the following:

```clojure
(add-tap #'submit)

(require '[portal.api :as p])
(p/open {:value tap-list})

(tap> :hello)
```

Which looks something like:

![Table Tap List](https://user-images.githubusercontent.com/1986211/164960811-b7ebfa17-05ab-4b6b-be5f-f3b9b0f742d7.png)
