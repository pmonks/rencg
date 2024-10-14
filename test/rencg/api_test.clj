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
            [rencg.api    :refer [re-named-groups re-matches-ncg re-find-ncg re-seq-ncg]]))

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
    (is (= {:start 0 :end 0 :match ""}    (re-matches-ncg #".*"  "")))
    (is (= {:start 0 :end 3 :match "foo"} (re-matches-ncg #"foo" "foo"))))
  (testing "Matches that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {:start 0 :end 3 :match "bar"} (re-matches-ncg #"(?<foo>foo)?.*" "bar"))))
  (testing "Matches that do have named-capturing groups, and some or all of them have values"
    (is (= {:start 0 :end  3 :match "foo"    "foo" "foo"}                                              (re-matches-ncg #"(?<foo>foo)"                "foo")))
    (is (= {:start 0 :end  6 :match "foofoo" "foo" "foo"}                                              (re-matches-ncg #"(?<foo>foo)+"               "foofoo")))                    ; Note: start and end indexes are for the entire match, not the named groups
    (is (= {:start 0 :end 24 :match "foobarfoobarfoobarfoobar" "foo" "foo" "bar" "bar"}                (re-matches-ncg #"((?<foo>foo)|(?<bar>bar))+" "foobarfoobarfoobarfoobar")))  ; Note: Java only matches a single value for a NCG, even if the named group is found multiple times
    (is (= {:start 0 :end  6 :match "foobar" "content" "foobar"}                                       (re-matches-ncg #"(?<content>.*)"             "foobar")))
    (is (= {:start 0 :end  6 :match "Apache" "name" "Apache"}                                          (re-matches-ncg apache-re                     "Apache")))
    (is (= {:start 0 :end  6 :match "apache" "name" "apache"}                                          (re-matches-ncg apache-re                     "apache")))
    (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-matches-ncg apache-re                     "Apache 2.0")))
    (is (= {:start 0 :end  8 :match "Apache 1" "name" "Apache" "version" "1"}                          (re-matches-ncg apache-re                     "Apache 1")))
    (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-matches-ncg apache-re                     "Apache Software License Version 2"))))
  (testing "Matches with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                          (re-matches-ncg #"foo"         ""                                  ncgs)))
      (is (nil?                                                                                          (re-matches-ncg #"(?<foo>foo)" ""                                  ncgs)))
      (is (= {:start 0 :end 3  :match "foo"}                                                             (re-matches-ncg #"foo"         "foo"                               ncgs)))
      ; These cases make more sense
      (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-matches-ncg #"(?<foo>foo)" "foo"                               #{"foo"})))
      (is (nil?                                                                                          (re-matches-ncg apache-re      "Mozilla"                           ncgs)))
      (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-matches-ncg apache-re      "Apache"                            ncgs)))
      (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-matches-ncg apache-re      "apache"                            ncgs)))
      (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-matches-ncg apache-re      "Apache 2.0"                        ncgs)))
      (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-matches-ncg apache-re      "Apache 1"                          ncgs)))
      (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-matches-ncg apache-re      "Apache Software License Version 2" ncgs))))))

(deftest re-find-ncg-tests
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-find
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
    (is (= {:start 0 :end 0 :match ""}    (re-find-ncg #".*"  "")))
    (is (= {:start 0 :end 3 :match "foo"} (re-find-ncg #"foo" "foo"))))
  (testing "Finds that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {:start 0 :end 3 :match "bar"} (re-find-ncg #"(?<foo>foo)?.*" "bar"))))
  (testing "Finds that do have named-capturing groups, and some or all of them have values"
    (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-find-ncg #"(?<foo>foo)"    "foo")))
    (is (= {:start 7 :end 10 :match "foo" "foo" "foo"}                                                 (re-find-ncg #"(?<foo>foo)"    "prefix foo suffix")))
    (is (= {:start 0 :end 6  :match "foobar" "content" "foobar"}                                       (re-find-ncg #"(?<content>.*)" "foobar")))
    (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-find-ncg apache-re         "Apache")))
    (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-find-ncg apache-re         "apache")))
    (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-find-ncg apache-re         "Apache 2.0")))
    (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-find-ncg apache-re         "Apache 1")))
    (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find-ncg apache-re         "Apache Software License Version 2")))
    (is (= {:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find-ncg apache-re         "prefix Apache Software License Version 2 suffix"))))
  (testing "Repeated finds, reusing the same matcher"
    (let [re   #"(?<foo>foo)"
          s    "foofoofoo"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {:start 0 :end 3 :match "foo" "foo" "foo"} (re-find-ncg m ncgs)))   ; First foo in s
      (is (= {:start 3 :end 6 :match "foo" "foo" "foo"} (re-find-ncg m ncgs)))   ; Second foo in s
      (is (= {:start 6 :end 9 :match "foo" "foo" "foo"} (re-find-ncg m ncgs)))   ; Third foo
      (is (nil?            (re-find-ncg m ncgs))))  ; No more foos in s
    (let [re   #"(?<foo>foo)"
          s    "prefix foo interstitial text foo suffix"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {:start 7  :end 10 :match "foo" "foo" "foo"} (re-find-ncg m ncgs)))    ; First foo in s
      (is (= {:start 29 :end 32 :match "foo" "foo" "foo"} (re-find-ncg m ncgs)))    ; Second foo in s
      (is (nil?            (re-find-ncg m ncgs)))))  ; No more foos in s
  (testing "Finds with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                          (re-find-ncg #"foo"         ""                                                ncgs)))
      (is (nil?                                                                                          (re-find-ncg #"(?<foo>foo)" ""                                                ncgs)))
      (is (nil?                                                                                          (re-find-ncg #"(?<foo>foo)" "bar"                                             ncgs)))
      (is (= {:start 0 :end 3  :match "foo"}                                                             (re-find-ncg #"foo"         "foo"                                             ncgs)))
      (is (= {:start 7 :end 10 :match "foo"}                                                             (re-find-ncg #"foo"         "prefix foo suffix"                               ncgs)))
      ; These cases make more sense
      (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-find-ncg #"(?<foo>foo)" "foo"                                             #{"foo"})))
      (is (nil?                                                                                          (re-find-ncg apache-re      "Mozilla"                                         ncgs)))
      (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-find-ncg apache-re      "Apache"                                          ncgs)))
      (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-find-ncg apache-re      "apache"                                          ncgs)))
      (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-find-ncg apache-re      "Apache 2.0"                                      ncgs)))
      (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-find-ncg apache-re      "Apache 1"                                        ncgs)))
      (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find-ncg apache-re      "Apache Software License Version 2"               ncgs)))
      (is (= {:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find-ncg apache-re      "prefix Apache Software License Version 2 suffix" ncgs))))))

(deftest re-seq-ncg-test
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-seq
    (is (thrown? java.lang.NullPointerException (re-seq-ncg nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-seq-ncg #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-seq-ncg nil   ""))))
  (testing "Non-matching seqs that don't have named-capturing groups"
    (is (nil? (re-seq-ncg #"foo"   "")))
    (is (nil? (re-seq-ncg #"foo"   "bar")))
    (is (nil? (re-seq-ncg #"(foo)" ""))))
  (testing "Non-matching seqs that do have named-capturing groups"
    (is (nil? (re-seq-ncg #"(?<foo>foo)" "")))
    (is (nil? (re-seq-ncg apache-re      "Mozilla"))))
  (testing "Matching seqs that don't have named-capturing groups"
    (is (= '({:start 0 :end 0 :match ""})                                   (re-seq-ncg #".*"  "")))
    (is (= '({:start 0 :end 3 :match "foo"})                                (re-seq-ncg #"foo" "foo")))
    (is (= '({:start 0 :end 3 :match "foo"} {:start 3 :end 6 :match "foo"}) (re-seq-ncg #"foo" "foofoo"))))
  (testing "Matching seqs that do have named-capturing groups, but they don't have values in the matched text"
    (is (= '({:start 0 :end 3 :match "bar"} {:start 3 :end 3 :match ""}) (re-seq-ncg #"(?<foo>foo)?.*" "bar"))))  ; Note: .* matches twice here - compare to (re-seq #".*" "bar")
  (testing "Matching seqs that do have named-capturing groups, and some or all of them have values"
    (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"})                                                    (re-seq-ncg #"(?<foo>foo)"    "foo")))
    (is (= '({:start 7 :end 10 :match "foo" "foo" "foo"})                                                    (re-seq-ncg #"(?<foo>foo)"    "prefix foo suffix")))
    (is (= '({:start 0 :end 6  :match "foobar" "content" "foobar"} {:start 6 :end 6 :match "" "content" ""}) (re-seq-ncg #"(?<content>.*)" "foobar")))  ; Note: .* matches twice here - compare to (re-seq #".*" "foobar")
    (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"})                                             (re-seq-ncg apache-re         "Apache")))
    (is (= '({:start 0 :end 6  :match "apache" "name" "apache"})                                             (re-seq-ncg apache-re         "apache")))
    (is (= '({:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"})                         (re-seq-ncg apache-re         "Apache 2.0")))
    (is (= '({:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"})                             (re-seq-ncg apache-re         "Apache 1")))
    (is (= '({:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"})    (re-seq-ncg apache-re         "Apache Software License Version 2")))
    (is (= '({:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"})    (re-seq-ncg apache-re         "prefix Apache Software License Version 2 suffix"))))
  (testing "Matching seqs with multiple matches"
    (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"} {:start 3 :end 6 :match "foo" "foo" "foo"})
           (re-seq-ncg #"(?<foo>foo)" "foofoo")))
    (is (= '({:start 7 :end 10 :match "foo" "foo" "foo"} {:start 29 :end 32 :match "foo" "foo" "foo"})
           (re-seq-ncg #"(?<foo>foo)" "prefix foo interstitial text foo suffix")))
    (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"} {:start 6 :end 12 :match "apache" "name" "apache"})
           (re-seq-ncg apache-re      "Apacheapache")))
    (is (= '({:start 0 :end 10 :match "apache 2.0" "name" "apache" "version" "2.0"} {:start 11 :end 21 :match "Apache 2.0" "name" "Apache" "version" "2.0"})
           (re-seq-ncg apache-re      "apache 2.0 Apache 2.0")))
    (is (= '({:start 7 :end 15 :match "Apache 1" "name" "Apache" "version" "1"} {:start 34 :end 69 :match "Apache Software License Version 2.0" "name" "Apache" "version" "2.0"})
           (re-seq-ncg apache-re      "prefix Apache 1 interstitial text Apache Software License Version 2.0 suffix"))))
  (testing "Matching seqs with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                             (re-seq-ncg #"foo"         ""                                                ncgs)))
      (is (nil?                                                                                             (re-seq-ncg #"(?<foo>foo)" ""                                                ncgs)))
      (is (nil?                                                                                             (re-seq-ncg #"(?<foo>foo)" "bar"                                             ncgs)))
      (is (= '({:start 0 :end 3  :match "foo"})                                                             (re-seq-ncg #"foo"         "foo"                                             ncgs)))
      (is (= '({:start 7 :end 10 :match "foo"})                                                             (re-seq-ncg #"foo"         "prefix foo suffix"                               ncgs)))
      ; These cases make more sense
      (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"})                                                 (re-seq-ncg #"(?<foo>foo)" "foo"                                             #{"foo"})))
      (is (nil?                                                                                             (re-seq-ncg apache-re      "Mozilla"                                         ncgs)))
      (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"})                                          (re-seq-ncg apache-re      "Apache"                                          ncgs)))
      (is (= '({:start 0 :end 6  :match "apache" "name" "apache"})                                          (re-seq-ncg apache-re      "apache"                                          ncgs)))
      (is (= '({:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"})                      (re-seq-ncg apache-re      "Apache 2.0"                                      ncgs)))
      (is (= '({:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"})                          (re-seq-ncg apache-re      "Apache 1"                                        ncgs)))
      (is (= '({:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"}) (re-seq-ncg apache-re      "Apache Software License Version 2"               ncgs)))
      (is (= '({:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"}) (re-seq-ncg apache-re      "prefix Apache Software License Version 2 suffix" ncgs))))))

