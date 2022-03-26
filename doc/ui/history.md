# History

The top-left arrow buttons (`⭠` `⭢`) can be used to navigate through history.
History is built up from commands pushing values into it. For example, anytime a
value is double clicked,
[`clojure.datafy/nav`](https://clojuredocs.org/clojure.datafy/nav) is applied to
that value and the result is pushed onto the history stack. All commands that
produce a new value will do the same.

To quickly navigation through history, checkout the following commands:

- `portal.ui.commands/history-back`
- `portal.ui.commands/history-forward`
- `portal.ui.commands/history-first`
- `portal.ui.commands/history-last`

## Portal Atom

In addition to getting the selected value back in the repl, the portal atom also
allows direct manipulation of portal [history](#history). For example:

```clojure
(def a (p/open))

; push the value 0 into portal history
(reset! a 0)

@a ;=> 0 - get current value

; inc the current value in portal
(swap! a inc)

@a ;=> 1 - get current value
```
