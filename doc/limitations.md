# Limitations

Since the Portal UI is implemented in ClojureScript, it also adopts some of its
platform limitations.

## Dates

Dates captured from your host platform are interpreted as `js/Date` in the
Portal UI. Therefore, as documents by this [ClojureScript issue][date-issue],
they are potentially interpreted against a different calendar.

## Longs

Since JavaScript doesn't support the full range of numbers provided by longs,
integers outside of a certain range must be boxed and shipped as string to the
UI. This precludes them from participating in viewers that expect `js/Number`.


[date-issue]: https://github.com/clojure/clojurescript-site/issues/367