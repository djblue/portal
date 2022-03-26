# Emacs

If you are an emacs + cider user and would like tighter integration with portal,
the following section may be of interest to you.

``` emacs-lisp
;; Leverage an existing cider nrepl connection to evaluate portal.api functions
;; and map them to convenient key bindings.

(defun portal.api/open ()
  (interactive)
  (cider-nrepl-sync-request:eval
   "(require 'portal.api) (portal.api/tap) (portal.api/open)"))

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
