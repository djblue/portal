# Emacs

If you are an emacs + cider user and would like tighter integration with portal,
the following section may be of interest to you.

``` emacs-lisp
;; Leverage an existing cider nrepl connection to evaluate portal.api functions
;; and map them to convenient key bindings.

;; def portal to the dev namespace to allow dereferencing via @dev/portal
(defun portal.api/open ()
  (interactive)
  (cider-nrepl-sync-request:eval
    "(do (ns dev) (def portal ((requiring-resolve 'portal.api/open))) (add-tap (requiring-resolve 'portal.api/submit)))"))

(defun portal.api/clear ()
  (interactive)
  (cider-nrepl-sync-request:eval "(portal.api/clear)"))

(defun portal.api/close ()
  (interactive)
  (cider-nrepl-sync-request:eval "(portal.api/close)"))

;; Example key mappings for doom emacs
(map! :map clojure-mode-map
      ;; cmd  + o
      :n "s-o" #'portal.api/open
      ;; ctrl + l
      :n "C-l" #'portal.api/clear)

;; NOTE: You do need to have portal on the class path and the easiest way I know
;; how is via a clj user or project alias.
(setq cider-clojure-cli-global-options "-A:portal")
```

## xwidget-webkit embed

![Screen Shot 2022-10-15 at 2 45 29 PM](https://user-images.githubusercontent.com/1986211/196008954-f3aeac5f-0a5f-4c90-bd82-22f727beda57.png)

If you would like to run the Portal UI in emacs itself, you can do so with the
following code.

> **Note** You need a build of emacs with [Embedded-WebKit-Widgets][1]

```clojure
(portal.api/open {:launcher :emacs})
```

> **Warning** I haven't tested this extensively so you might run into some weird
> issues

[1]: https://www.gnu.org/software/emacs/manual/html_node/emacs/Embedded-WebKit-Widgets.html
