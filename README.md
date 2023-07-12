| | | | |
|---:|:---:|:---:|:---:|
| [**main**](https://github.com/pmonks/rencg/tree/main) | [![CI](https://github.com/pmonks/rencg/workflows/CI/badge.svg?branch=main)](https://github.com/pmonks/rencg/actions?query=workflow%3ACI+branch%3Amain) | [![Dependencies](https://github.com/pmonks/rencg/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/rencg/actions?query=workflow%3Adependencies+branch%3Amain) | [![Vulnerabilities](https://github.com/pmonks/rencg/workflows/vulnerabilities/badge.svg?branch=main)](https://pmonks.github.io/rencg/nvd/dependency-check-report.html) |
| [**dev**](https://github.com/pmonks/rencg/tree/dev)  | [![CI](https://github.com/pmonks/rencg/workflows/CI/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/rencg/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3Adependencies+branch%3Adev) | [![Vulnerabilities](https://github.com/pmonks/rencg/workflows/vulnerabilities/badge.svg?branch=dev)](https://github.com/pmonks/rencg/actions?query=workflow%3Avulnerabilities+branch%3Adev) |

[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/rencg)](https://clojars.org/com.github.pmonks/rencg/) [![Open Issues](https://img.shields.io/github/issues/pmonks/rencg.svg)](https://github.com/pmonks/rencg/issues) [![License](https://img.shields.io/github/license/pmonks/rencg.svg)](https://github.com/pmonks/rencg/blob/main/LICENSE)


# rencg

A micro-library for Clojure that provides first class support for accessing the values of [named-capturing groups](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#groupname) in regular expressions. It has no dependencies, other than on Clojure and any supported JVM, and is [less than 100 lines of code](https://github.com/pmonks/rencg/blob/main/src/rencg/api.clj).

#### Why not [rufoa/named-re](https://github.com/rufoa/named-re)?

Because that library [monkey patches core Clojure](https://github.com/rufoa/named-re/blob/master/src/named_re/core.clj#L26-L32), which may break other code.

## Installation

`rencg` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/rencg).

### Trying it Out

#### Clojure CLI

```shell
$ # Where #.#.# is replaced with an actual version number (see badge above)
$ clj -Sdeps '{:deps {com.github.pmonks/rencg {:mvn/version "#.#.#"}}}'
```

#### Leiningen

```shell
$ lein try com.github.pmonks/rencg
```

#### deps-try

```shell
$ deps-try com.github.pmonks/rencg
```

## Usage

[API documentation is available here](https://pmonks.github.io/rencg/), or [here on cljdoc](https://cljdoc.org/d/com.github.pmonks/rencg/), and the [unit tests](https://github.com/pmonks/rencg/blob/main/test/rencg/api_test.clj) are also worth perusing to see worked examples.

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/rencg/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/rencg/issues)

[Code of Conduct](https://github.com/pmonks/rencg/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), with the caveat that the permanent branches are called `main` and `dev`, and any changes to the `main` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

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
