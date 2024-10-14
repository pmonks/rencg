| | | | |
|---:|:---:|:---:|:---:|
| [**release**](https://github.com/pmonks/rencg/tree/release) | [![CI](https://github.com/pmonks/rencg/actions/workflows/ci.yml/badge.svg?branch=release)](https://github.com/pmonks/rencg/actions?query=workflow%3ACI+branch%3Arelease) | [![Dependencies](https://github.com/pmonks/rencg/actions/workflows/dependencies.yml/badge.svg?branch=release)](https://github.com/pmonks/rencg/actions?query=workflow%3Adependencies+branch%3Arelease) | [![Vulnerabilities](https://github.com/pmonks/rencg/actions/workflows/vulnerabilities.yml/badge.svg?branch=release)](https://pmonks.github.io/rencg/nvd/dependency-check-report.html) |
| [**dev**](https://github.com/pmonks/rencg/tree/dev)  | [![CI](https://github.com/pmonks/rencg/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/rencg/actions/workflows/dependencies.yml/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3Adependencies+branch%3Adev) | [![Vulnerabilities](https://github.com/pmonks/rencg/actions/workflows/vulnerabilities.yml/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3Avulnerabilities+branch%3Adev) |

[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/rencg)](https://clojars.org/com.github.pmonks/rencg/) [![Open Issues](https://img.shields.io/github/issues/pmonks/rencg.svg)](https://github.com/pmonks/rencg/issues) [![License](https://img.shields.io/github/license/pmonks/rencg.svg)](https://github.com/pmonks/rencg/blob/release/LICENSE)


# rencg

A micro-library for Clojure that provides first class support for accessing the values of [named-capturing groups](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#groupname) in regular expressions. It has no dependencies, other than on Clojure and the JVM versions it supports, and is [only around 100 lines of code](https://github.com/pmonks/rencg/blob/release/src/rencg/).

#### Does `rencg` work on older JVMs that don't have the [`.namedGroups()` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#namedGroups())?

Yes. On older (pre v20) JVMs, `rencg` [falls back on manually parsing regexes to determine the named capturing groups](https://github.com/pmonks/rencg/blob/release/src/rencg/non_native.clj).

#### Why not [rufoa/named-re](https://github.com/rufoa/named-re)?

Because that library [monkey patches core Clojure](https://github.com/rufoa/named-re/blob/master/src/named_re/core.clj#L26-L32), which may break other code.

## Installation

`rencg` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/rencg).

### Trying it Out

#### Clojure CLI

```shell
$ clj -Sdeps '{:deps {com.github.pmonks/rencg {:mvn/version "RELEASE"}}}'
```

#### Leiningen

```shell
$ lein try com.github.pmonks/rencg
```

#### deps-try

```shell
$ deps-try com.github.pmonks/rencg
```

### Demo

```clojure
(require '[rencg.api :as rencg])


;; re-matches-ncg - for when you want to match the entire input
(rencg/re-matches-ncg #"(?<foo>foo)" "bar")
;=> nil

(rencg/re-matches-ncg #"(?<foo>foo)" "foo")
;=> {:start 0, :end 3, :match "foo", "foo" "foo"}

(rencg/re-matches-ncg #"(?<foo>foo)+" "foofoo")
;=> {:start 0, :end 6, :match "foofoo", "foo" "foo"}

; Note: Java named capturing groups only capture a single value from the input, even if the
; group is present multiple times. Also, the start and end indexes are for the entire match,
; not where the named capturing groups are found (obviously, since there may be many named
; capturing groups all of which have different start and end indexes).

(rencg/re-matches-ncg #"((?<foo>foo)|(?<bar>bar))+" "foobarfoobarfoobarfoobar")
;=> {:start 0, :end 24, :match "foobarfoobarfoobarfoobar", "foo" "foo", "bar" "bar"}

; This last example also shows the value of using named capturing groups instead of numbered
; capturing groups (the latter being brittle, since non-named groups conflate grouping and
; capture)


;; re-seq-ncg - for when you want all matches of a named capturing group that exist within
;;              the input
(rencg/re-seq-ncg #"((?<foo>foo)|(?<bar>bar))" "foobarfoobarfoobarfoobar")
;=> ({:start 0, :end 3, :match "foo", "foo" "foo"}
;    {:start 3, :end 6, :match "bar", "bar" "bar"}
;    {:start 6, :end 9, :match "foo", "foo" "foo"}
;    {:start 9, :end 12, :match "bar", "bar" "bar"}
;    {:start 12, :end 15, :match "foo", "foo" "foo"}
;    {:start 15, :end 18, :match "bar", "bar" "bar"}
;    {:start 18, :end 21, :match "foo", "foo" "foo"}
;    {:start 21, :end 24, :match "bar", "bar" "bar"})


;; re-find-ncg - for when you want to extract something specific from the input, using
;;               standard Clojure map lookups
(get (rencg/re-find-ncg #"(?i)(?<foo>foo)" "THIS IS SOME TEXT WITH FOO IN IT") "foo")
;=> "FOO"
```

## Usage

[API documentation is available here](https://pmonks.github.io/rencg/), or [here on cljdoc](https://cljdoc.org/d/com.github.pmonks/rencg/), and the [unit tests](https://github.com/pmonks/rencg/blob/release/test/rencg/api_test.clj) are also worth perusing to see worked examples.

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/rencg/blob/release/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/rencg/issues)

[Code of Conduct](https://github.com/pmonks/rencg/blob/release/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), and the permanent branches are called `release` and `dev`.  Any changes to the `release` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `release` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `release` will be rejected.

### Build Tasks

`rencg` uses [`tools.build`](https://clojure.org/guides/tools_build). You can get a list of available tasks by running:

```
clojure -A:deps -T:build help/doc
```

Of particular interest are:

* `clojure -T:build test` - run the unit tests
* `clojure -T:build lint` - run the linters (clj-kondo and eastwood)
* `clojure -T:build ci` - run the full CI suite (check for outdated dependencies, run the unit tests, run the linters)
* `clojure -T:build install` - build the JAR and install it locally (e.g. so you can test it with downstream code)

Please note that the `release` and `deploy` tasks are restricted to the core development team (and will not function if you run them yourself).

## License

Copyright © 2023 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
