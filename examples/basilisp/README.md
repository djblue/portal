# Basilisp Guide

Basic guide for using Portal with [Basilisp][basilisp].

## Setup

```bash
python -m venv .py 
.py/bin/pip install basilisp djblue.portal
.py/bin/basilisp repl
```

## Basic Usage

```clojure
(require '[portal.api :as p])
(p/open)
(add-tap #'p/submit)
(tap> :hello-world)
```

[See more](../../README.md#api)

[basilisp]: https://github.com/basilisp-lang/basilisp