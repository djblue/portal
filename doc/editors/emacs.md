# Emacs

## CIDER

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
(setq cider-clojure-cli-aliases ":portal")
```

## xwidget-webkit embed

![Screen Shot 2022-10-15 at 2 45 29 PM](https://user-images.githubusercontent.com/1986211/196008954-f3aeac5f-0a5f-4c90-bd82-22f727beda57.png)

If you would like to run the Portal UI in emacs itself, you can do so with the
following code.

> **Note**
> You need a build of emacs with [Embedded-WebKit-Widgets][1]
> Also, this method only works if you're using [emacs server](https://www.gnu.org/software/emacs/manual/html_node/emacs/Emacs-Server.html) functionality as it relies on `emacsclient` command

```clojure
(portal.api/open {:launcher :emacs})
```

> **Warning**
> I haven't tested this extensively so you might run into some weird
> issues

[1]: https://www.gnu.org/software/emacs/manual/html_node/emacs/Embedded-WebKit-Widgets.html

## Monroe & xwidget-webkit

For [Monroe](https://github.com/sanel/monroe) nREPL client users.

### The code

Add the following to your Emacs config:

```elisp
; -*- lexical-binding: t; -*-
;; The line above is really important, otherwise things will not work.

;; A little helper to simplify communication between Emacs and the nREPL process
(defun monroe-eval-code-and-callback-with-value (code-str on-value)
  (monroe-send-eval-string
   code-str
   (lambda (response)
     (condition-case err
         (monroe-dbind-response response
                                (value status id)
                                (when value (funcall on-value value))
                                (when (member "done" status)
                                  (remhash id monroe-requests)))
       (error (message "Eval error %s" err))))))



(defun monroe-launch-portal ()
  (interactive)
  (monroe-eval-code-and-callback-with-value
   "(do
     (require 'portal.api)
     (let [url (portal.api/url (portal.api/open {:launcher false}))]
       (add-tap #'portal.api/submit)
       url)"
   (lambda (value)
     ;; value is a raw string, so we need to remove " from it
     (let ((url (string-replace "\"" "" value)))
       (message "Opening portal %s" url)
       (xwidget-webkit-browse-url url)))))

```

### Usage

Then in the Monroe REPL or Clojure buffer with an active Monroe session run `M-x monroe-launch-portal`.

> **Note**
>
> This is just a jump-off point, you might want to customize this function to your
> liking, e.g. run the code in your `user` namespace, set a different theme and so on.
> See CIDER section for how you can extend it further.
