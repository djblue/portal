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

## Lazy Values

Lazy values that contain exceptions will cause Portal's internals to break if
allowed to flow into the tap-list. By trying to partially realize lazy values
before accepting them, and rejecting any exceptional values, Portal can keep
working. However, this only applies to the default tap-list. Since Portal can be
opened with any value, it is still possible to break that instance of Portal,
but it should be contained to that specific instance.

[date-issue]: https://github.com/clojure/clojurescript-site/issues/367