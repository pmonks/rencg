;
; Copyright © 2023 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns rencg.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [rencg.api    :refer [re-named-groups re-matches-ncg re-find-ncg]]))

(def apache-re #"(?i)(?<name>Apache)(\s+Software)?(\s+License(s)?(\s*[,-])?)?(\s+V(ersion)?)?\s*(?<version>\d+(\.\d+)?)?")

(deftest re-named-groups-tests
  (testing "Nil, empty or blank regexes"
    (is (nil?  (re-named-groups nil)))
    (is (= #{} (re-named-groups #"")))
    (is (= #{} (re-named-groups #"      ")))
    (is (= #{} (re-named-groups #"\n\t\r"))))
  (testing "Regexes with no named-capturing groups"
    (is (= #{} (re-named-groups #".*")))
    (is (= #{} (re-named-groups #"(.*)"))))
  (testing "Regexes with named-capturing groups"
    (is (= #{"namedGroup"}             (re-named-groups #"(?<namedGroup>.*)")))
    (is (= #{"givenName" "familyName"} (re-named-groups #"(?<givenName>.*)\s+(?<familyName>.*)")))
    (is (= #{"name" "version"}         (re-named-groups apache-re)))
    (is (= #{"outer" "inner"}          (re-named-groups #"(?<outer>foo(?<inner>bar)?)")))                 ; Nested named groups
    (is (= #{"outer" "inner"}          (re-named-groups #"(?<outer>foo)(\s+blah(?<inner>\s+bar)?)?")))))  ; Nested named groups, but in different groups

(deftest re-matches-ncg-tests
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-matches
    (is (thrown? java.lang.NullPointerException (re-matches-ncg nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-matches-ncg #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-matches-ncg nil   ""))))
  (testing "Non-matches that don't have named-capturing groups"
    (is (nil? (re-matches-ncg #"foo"   "")))
    (is (nil? (re-matches-ncg #"foo"   "bar")))
    (is (nil? (re-matches-ncg #"(foo)" ""))))
  (testing "Non-matches that do have named-capturing groups"
    (is (nil? (re-matches-ncg #"(?<foo>foo)" "")))
    (is (nil? (re-matches-ncg apache-re      "Mozilla"))))
  (testing "Matches that don't have named-capturing groups"
    (is (= {} (re-matches-ncg #".*"  "")))
    (is (= {} (re-matches-ncg #"foo" "foo"))))
  (testing "Matches that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {} (re-matches-ncg #"(?<foo>foo)?.*" "bar"))))
  (testing "Matches that do have named-capturing groups, and some or all of them have values"
    (is (= {"foo" "foo"}                      (re-matches-ncg #"(?<foo>foo)"    "foo")))
    (is (= {"content" "foobar"}               (re-matches-ncg #"(?<content>.*)" "foobar")))
    (is (= {"name" "Apache"}                  (re-matches-ncg apache-re         "Apache")))
    (is (= {"name" "apache"}                  (re-matches-ncg apache-re         "apache")))
    (is (= {"name" "Apache", "version" "2.0"} (re-matches-ncg apache-re         "Apache 2.0")))
    (is (= {"name" "Apache", "version" "1"}   (re-matches-ncg apache-re         "Apache 1")))
    (is (= {"name" "Apache", "version" "2"}   (re-matches-ncg apache-re         "Apache Software License Version 2"))))
  (testing "Matches with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                 (re-matches-ncg #"foo"         ""                                  ncgs)))
      (is (nil?                                 (re-matches-ncg #"(?<foo>foo)" ""                                  ncgs)))
      (is (= {}                                 (re-matches-ncg #"foo"         "foo"                               ncgs)))
      ; These cases make more sense
      (is (= {"foo" "foo"}                      (re-matches-ncg #"(?<foo>foo)" "foo"                               #{"foo"})))
      (is (nil?                                 (re-matches-ncg apache-re      "Mozilla"                           ncgs)))
      (is (= {"name" "Apache"}                  (re-matches-ncg apache-re      "Apache"                            ncgs)))
      (is (= {"name" "apache"}                  (re-matches-ncg apache-re      "apache"                            ncgs)))
      (is (= {"name" "Apache", "version" "2.0"} (re-matches-ncg apache-re      "Apache 2.0"                        ncgs)))
      (is (= {"name" "Apache", "version" "1"}   (re-matches-ncg apache-re      "Apache 1"                          ncgs)))
      (is (= {"name" "Apache", "version" "2"}   (re-matches-ncg apache-re      "Apache Software License Version 2" ncgs))))))

(deftest re-find-ncg-tests
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-matches
    (is (thrown? java.lang.NullPointerException (re-find-ncg nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-find-ncg #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-find-ncg nil   ""))))
  (testing "Non-finds that don't have named-capturing groups"
    (is (nil? (re-find-ncg #"foo"   "")))
    (is (nil? (re-find-ncg #"foo"   "bar")))
    (is (nil? (re-find-ncg #"(foo)" ""))))
  (testing "Non-finds that do have named-capturing groups"
    (is (nil? (re-find-ncg #"(?<foo>foo)" "")))
    (is (nil? (re-find-ncg apache-re      "Mozilla"))))
  (testing "Finds that don't have named-capturing groups"
    (is (= {} (re-find-ncg #".*"  "")))
    (is (= {} (re-find-ncg #"foo" "foo"))))
  (testing "Finds that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {} (re-find-ncg #"(?<foo>foo)?.*" "bar"))))
  (testing "Finds that do have named-capturing groups, and some or all of them have values"
    (is (= {"foo" "foo"}                      (re-find-ncg #"(?<foo>foo)"    "foo")))
    (is (= {"foo" "foo"}                      (re-find-ncg #"(?<foo>foo)"    "prefix foo suffix")))
    (is (= {"content" "foobar"}               (re-find-ncg #"(?<content>.*)" "foobar")))
    (is (= {"name" "Apache"}                  (re-find-ncg apache-re         "Apache")))
    (is (= {"name" "apache"}                  (re-find-ncg apache-re         "apache")))
    (is (= {"name" "Apache", "version" "2.0"} (re-find-ncg apache-re         "Apache 2.0")))
    (is (= {"name" "Apache", "version" "1"}   (re-find-ncg apache-re         "Apache 1")))
    (is (= {"name" "Apache", "version" "2"}   (re-find-ncg apache-re         "Apache Software License Version 2")))
    (is (= {"name" "Apache", "version" "2"}   (re-find-ncg apache-re         "prefix Apache Software License Version 2 suffix"))))
  (testing "Repeated finds, reusing the same matcher"
    (let [re   #"(?<foo>foo)"
          s    "foofoofoo"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {"foo" "foo"} (re-find-ncg m ncgs)))   ; First foo in s
      (is (= {"foo" "foo"} (re-find-ncg m ncgs)))   ; Second foo in s
      (is (= {"foo" "foo"} (re-find-ncg m ncgs)))   ; Third foo
      (is (nil?            (re-find-ncg m ncgs))))  ; No more foos in s
    (let [re   #"(?<foo>foo)"
          s    "prefix foo interstitial text foo suffix"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {"foo" "foo"} (re-find-ncg m ncgs)))    ; First foo in s
      (is (= {"foo" "foo"} (re-find-ncg m ncgs)))    ; Second foo in s
      (is (nil?            (re-find-ncg m ncgs)))))  ; No more foos in s
  (testing "Finds with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                 (re-find-ncg #"foo"         ""                                                ncgs)))
      (is (nil?                                 (re-find-ncg #"(?<foo>foo)" ""                                                ncgs)))
      (is (nil?                                 (re-find-ncg #"(?<foo>foo)" "bar"                                             ncgs)))
      (is (= {}                                 (re-find-ncg #"foo"         "foo"                                             ncgs)))
      (is (= {}                                 (re-find-ncg #"foo"         "prefix foo suffix"                               ncgs)))
      ; These cases make more sense
      (is (= {"foo" "foo"}                      (re-find-ncg #"(?<foo>foo)" "foo"                                             #{"foo"})))
      (is (nil?                                 (re-find-ncg apache-re      "Mozilla"                                         ncgs)))
      (is (= {"name" "Apache"}                  (re-find-ncg apache-re      "Apache"                                          ncgs)))
      (is (= {"name" "apache"}                  (re-find-ncg apache-re      "apache"                                          ncgs)))
      (is (= {"name" "Apache", "version" "2.0"} (re-find-ncg apache-re      "Apache 2.0"                                      ncgs)))
      (is (= {"name" "Apache", "version" "1"}   (re-find-ncg apache-re      "Apache 1"                                        ncgs)))
      (is (= {"name" "Apache", "version" "2"}   (re-find-ncg apache-re      "Apache Software License Version 2"               ncgs)))
      (is (= {"name" "Apache", "version" "2"}   (re-find-ncg apache-re      "prefix Apache Software License Version 2 suffix" ncgs))))))
