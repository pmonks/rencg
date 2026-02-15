;
; Copyright © 2023 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

#_{:clj-kondo/ignore [:unresolved-namespace]}
(defn set-opts
  [opts]
  (assoc opts
         :lib          'com.github.pmonks/rencg
         :version      (pbr/calculate-version 1 0)
         :prod-branch  "release"
         :write-pom    true
         :validate-pom true
         :pom          {:description      "A micro-library for Clojure that provides first class support for named-capturing groups in regular expressions."
                        :url              "https://github.com/pmonks/rencg"
                        :licenses         [:license   {:name "MPL-2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}]
                        :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+rencg@gmail.com"}]
                        :scm              {:url                  "https://github.com/pmonks/rencg"
                                           :connection           "scm:git:git://github.com/pmonks/rencg.git"
                                           :developer-connection "scm:git:ssh://git@github.com/pmonks/rencg.git"
                                           :tag                  (tc/git-tag-or-hash)}
                        :issue-management {:system "github" :url "https://github.com/pmonks/rencg/issues"}}
         :codox        {:metadata         {:doc/format :markdown}}
         :eastwood     {:exclude-linters  [:no-ns-form-found]}))
