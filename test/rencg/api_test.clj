;
; Copyright © 2023 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns rencg.api-test
  (:refer-clojure :exclude [re-groups re-matches re-find re-seq])
  (:require [clojure.test :refer [deftest testing is]]
            [rencg.api    :refer [re-named-groups re-matches re-find re-seq]]))

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
    (is (= #{"a"}                      (re-named-groups #"(?<a>.*)")))
    (is (= #{"namedGroup"}             (re-named-groups #"(?<namedGroup>.*)")))
    (is (= #{"givenName" "familyName"} (re-named-groups #"(?<givenName>.*)\s+(?<familyName>.*)")))
    (is (= #{"name" "version"}         (re-named-groups apache-re)))
    (is (= #{"outer" "inner"}          (re-named-groups #"(?<outer>foo(?<inner>bar)?)")))                 ; Nested named groups
    (is (= #{"outer" "inner"}          (re-named-groups #"(?<outer>foo)(\s+blah(?<inner>\s+bar)?)?")))))  ; Nested named groups, but in different groups

(deftest re-matches-tests
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-matches
    (is (thrown? java.lang.NullPointerException (re-matches nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-matches #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-matches nil   ""))))
  (testing "Non-matches that don't have named-capturing groups"
    (is (nil? (re-matches #"foo"   "")))
    (is (nil? (re-matches #"foo"   "bar")))
    (is (nil? (re-matches #"(foo)" ""))))
  (testing "Non-matches that do have named-capturing groups"
    (is (nil? (re-matches #"(?<foo>foo)" "")))
    (is (nil? (re-matches apache-re      "Mozilla"))))
  (testing "Matches that don't have named-capturing groups"
    (is (= {:start 0 :end 0 :match ""}    (re-matches #".*"  "")))
    (is (= {:start 0 :end 3 :match "foo"} (re-matches #"foo" "foo"))))
  (testing "Matches that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {:start 0 :end 3 :match "bar"} (re-matches #"(?<foo>foo)?.*" "bar"))))
  (testing "Matches that do have named-capturing groups, and some or all of them have values"
    (is (= {:start 0 :end  3 :match "foo"    "foo" "foo"}                                              (re-matches #"(?<foo>foo)"                "foo")))
    (is (= {:start 0 :end  6 :match "foofoo" "foo" "foo"}                                              (re-matches #"(?<foo>foo)+"               "foofoo")))                    ; Note: start and end indexes are for the entire match, not the named groups
    (is (= {:start 0 :end 24 :match "foobarfoobarfoobarfoobar" "foo" "foo" "bar" "bar"}                (re-matches #"((?<foo>foo)|(?<bar>bar))+" "foobarfoobarfoobarfoobar")))  ; Note: Java only matches a single value for a NCG, even if the named group is found multiple times
    (is (= {:start 0 :end  6 :match "foobar" "content" "foobar"}                                       (re-matches #"(?<content>.*)"             "foobar")))
    (is (= {:start 0 :end  6 :match "Apache" "name" "Apache"}                                          (re-matches apache-re                     "Apache")))
    (is (= {:start 0 :end  6 :match "apache" "name" "apache"}                                          (re-matches apache-re                     "apache")))
    (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-matches apache-re                     "Apache 2.0")))
    (is (= {:start 0 :end  8 :match "Apache 1" "name" "Apache" "version" "1"}                          (re-matches apache-re                     "Apache 1")))
    (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-matches apache-re                     "Apache Software License Version 2"))))
  (testing "Matches with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                          (re-matches #"foo"         ""                                  ncgs)))
      (is (nil?                                                                                          (re-matches #"(?<foo>foo)" ""                                  ncgs)))
      (is (= {:start 0 :end 3  :match "foo"}                                                             (re-matches #"foo"         "foo"                               ncgs)))
      ; These cases make more sense
      (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-matches #"(?<foo>foo)" "foo"                               #{"foo"})))
      (is (nil?                                                                                          (re-matches apache-re      "Mozilla"                           ncgs)))
      (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-matches apache-re      "Apache"                            ncgs)))
      (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-matches apache-re      "apache"                            ncgs)))
      (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-matches apache-re      "Apache 2.0"                        ncgs)))
      (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-matches apache-re      "Apache 1"                          ncgs)))
      (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-matches apache-re      "Apache Software License Version 2" ncgs))))))

(deftest re-find-tests
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-find
    (is (thrown? java.lang.NullPointerException (re-find nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-find #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-find nil   ""))))
  (testing "Non-finds that don't have named-capturing groups"
    (is (nil? (re-find #"foo"   "")))
    (is (nil? (re-find #"foo"   "bar")))
    (is (nil? (re-find #"(foo)" ""))))
  (testing "Non-finds that do have named-capturing groups"
    (is (nil? (re-find #"(?<foo>foo)" "")))
    (is (nil? (re-find apache-re      "Mozilla"))))
  (testing "Finds that don't have named-capturing groups"
    (is (= {:start 0 :end 0 :match ""}    (re-find #".*"  "")))
    (is (= {:start 0 :end 3 :match "foo"} (re-find #"foo" "foo"))))
  (testing "Finds that do have named-capturing groups, but they don't have values in the matched text"
    (is (= {:start 0 :end 3 :match "bar"} (re-find #"(?<foo>foo)?.*" "bar"))))
  (testing "Finds that do have named-capturing groups, and some or all of them have values"
    (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-find #"(?<foo>foo)"    "foo")))
    (is (= {:start 7 :end 10 :match "foo" "foo" "foo"}                                                 (re-find #"(?<foo>foo)"    "prefix foo suffix")))
    (is (= {:start 0 :end 6  :match "foobar" "content" "foobar"}                                       (re-find #"(?<content>.*)" "foobar")))
    (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-find apache-re         "Apache")))
    (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-find apache-re         "apache")))
    (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-find apache-re         "Apache 2.0")))
    (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-find apache-re         "Apache 1")))
    (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find apache-re         "Apache Software License Version 2")))
    (is (= {:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find apache-re         "prefix Apache Software License Version 2 suffix"))))
  (testing "Repeated finds, reusing the same matcher"
    (let [re   #"(?<foo>foo)"
          s    "foofoofoo"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {:start 0 :end 3 :match "foo" "foo" "foo"} (re-find m ncgs)))   ; First foo in s
      (is (= {:start 3 :end 6 :match "foo" "foo" "foo"} (re-find m ncgs)))   ; Second foo in s
      (is (= {:start 6 :end 9 :match "foo" "foo" "foo"} (re-find m ncgs)))   ; Third foo
      (is (nil?            (re-find m ncgs))))  ; No more foos in s
    (let [re   #"(?<foo>foo)"
          s    "prefix foo interstitial text foo suffix"
          ncgs (re-named-groups re)
          m    (re-matcher re s)]
      (is (= {:start 7  :end 10 :match "foo" "foo" "foo"} (re-find m ncgs)))    ; First foo in s
      (is (= {:start 29 :end 32 :match "foo" "foo" "foo"} (re-find m ncgs)))    ; Second foo in s
      (is (nil?            (re-find m ncgs)))))  ; No more foos in s
  (testing "Finds with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                          (re-find #"foo"         ""                                                ncgs)))
      (is (nil?                                                                                          (re-find #"(?<foo>foo)" ""                                                ncgs)))
      (is (nil?                                                                                          (re-find #"(?<foo>foo)" "bar"                                             ncgs)))
      (is (= {:start 0 :end 3  :match "foo"}                                                             (re-find #"foo"         "foo"                                             ncgs)))
      (is (= {:start 7 :end 10 :match "foo"}                                                             (re-find #"foo"         "prefix foo suffix"                               ncgs)))
      ; These cases make more sense
      (is (= {:start 0 :end 3  :match "foo" "foo" "foo"}                                                 (re-find #"(?<foo>foo)" "foo"                                             #{"foo"})))
      (is (nil?                                                                                          (re-find apache-re      "Mozilla"                                         ncgs)))
      (is (= {:start 0 :end 6  :match "Apache" "name" "Apache"}                                          (re-find apache-re      "Apache"                                          ncgs)))
      (is (= {:start 0 :end 6  :match "apache" "name" "apache"}                                          (re-find apache-re      "apache"                                          ncgs)))
      (is (= {:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"}                      (re-find apache-re      "Apache 2.0"                                      ncgs)))
      (is (= {:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"}                          (re-find apache-re      "Apache 1"                                        ncgs)))
      (is (= {:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find apache-re      "Apache Software License Version 2"               ncgs)))
      (is (= {:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"} (re-find apache-re      "prefix Apache Software License Version 2 suffix" ncgs))))))

(deftest re-seq-test
  (testing "Nil regexes and/or input strings"
    ; Not a fan of throwing exceptions in these cases, but for better or worse this behaviour is compatible with clojure.core/re-seq
    (is (thrown? java.lang.NullPointerException (re-seq nil   nil)))
    (is (thrown? java.lang.NullPointerException (re-seq #".*" nil)))
    (is (thrown? java.lang.NullPointerException (re-seq nil   ""))))
  (testing "Non-matching seqs that don't have named-capturing groups"
    (is (nil? (re-seq #"foo"   "")))
    (is (nil? (re-seq #"foo"   "bar")))
    (is (nil? (re-seq #"(foo)" ""))))
  (testing "Non-matching seqs that do have named-capturing groups"
    (is (nil? (re-seq #"(?<foo>foo)" "")))
    (is (nil? (re-seq apache-re      "Mozilla"))))
  (testing "Matching seqs that don't have named-capturing groups"
    (is (= '({:start 0 :end 0 :match ""})                                   (re-seq #".*"  "")))
    (is (= '({:start 0 :end 3 :match "foo"})                                (re-seq #"foo" "foo")))
    (is (= '({:start 0 :end 3 :match "foo"} {:start 3 :end 6 :match "foo"}) (re-seq #"foo" "foofoo"))))
  (testing "Matching seqs that do have named-capturing groups, but they don't have values in the matched text"
    (is (= '({:start 0 :end 3 :match "bar"} {:start 3 :end 3 :match ""}) (re-seq #"(?<foo>foo)?.*" "bar"))))  ; Note: .* matches twice here - compare to (re-seq #".*" "bar")
  (testing "Matching seqs that do have named-capturing groups, and some or all of them have values"
    (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"})                                                    (re-seq #"(?<foo>foo)"    "foo")))
    (is (= '({:start 7 :end 10 :match "foo" "foo" "foo"})                                                    (re-seq #"(?<foo>foo)"    "prefix foo suffix")))
    (is (= '({:start 0 :end 6  :match "foobar" "content" "foobar"} {:start 6 :end 6 :match "" "content" ""}) (re-seq #"(?<content>.*)" "foobar")))  ; Note: .* matches twice here - compare to (re-seq #".*" "foobar")
    (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"})                                             (re-seq apache-re         "Apache")))
    (is (= '({:start 0 :end 6  :match "apache" "name" "apache"})                                             (re-seq apache-re         "apache")))
    (is (= '({:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"})                         (re-seq apache-re         "Apache 2.0")))
    (is (= '({:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"})                             (re-seq apache-re         "Apache 1")))
    (is (= '({:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"})    (re-seq apache-re         "Apache Software License Version 2")))
    (is (= '({:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"})    (re-seq apache-re         "prefix Apache Software License Version 2 suffix"))))
  (testing "Matching seqs with multiple matches"
    (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"} {:start 3 :end 6 :match "foo" "foo" "foo"})
           (re-seq #"(?<foo>foo)" "foofoo")))
    (is (= '({:start 7 :end 10 :match "foo" "foo" "foo"} {:start 29 :end 32 :match "foo" "foo" "foo"})
           (re-seq #"(?<foo>foo)" "prefix foo interstitial text foo suffix")))
    (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"} {:start 6 :end 12 :match "apache" "name" "apache"})
           (re-seq apache-re      "Apacheapache")))
    (is (= '({:start 0 :end 10 :match "apache 2.0" "name" "apache" "version" "2.0"} {:start 11 :end 21 :match "Apache 2.0" "name" "Apache" "version" "2.0"})
           (re-seq apache-re      "apache 2.0 Apache 2.0")))
    (is (= '({:start 7 :end 15 :match "Apache 1" "name" "Apache" "version" "1"} {:start 34 :end 69 :match "Apache Software License Version 2.0" "name" "Apache" "version" "2.0"})
           (re-seq apache-re      "prefix Apache 1 interstitial text Apache Software License Version 2.0 suffix"))))
  (testing "Matching seqs with pre-computed ncgs"
    (let [ncgs (re-named-groups apache-re)]
      ; Note: these cases are nonsensical since the names in ncgs don't correlate to the regexes, but we test these cases anyway to ensure reasonable behaviour
      (is (nil?                                                                                             (re-seq #"foo"         ""                                                ncgs)))
      (is (nil?                                                                                             (re-seq #"(?<foo>foo)" ""                                                ncgs)))
      (is (nil?                                                                                             (re-seq #"(?<foo>foo)" "bar"                                             ncgs)))
      (is (= '({:start 0 :end 3  :match "foo"})                                                             (re-seq #"foo"         "foo"                                             ncgs)))
      (is (= '({:start 7 :end 10 :match "foo"})                                                             (re-seq #"foo"         "prefix foo suffix"                               ncgs)))
      ; These cases make more sense
      (is (= '({:start 0 :end 3  :match "foo" "foo" "foo"})                                                 (re-seq #"(?<foo>foo)" "foo"                                             #{"foo"})))
      (is (nil?                                                                                             (re-seq apache-re      "Mozilla"                                         ncgs)))
      (is (= '({:start 0 :end 6  :match "Apache" "name" "Apache"})                                          (re-seq apache-re      "Apache"                                          ncgs)))
      (is (= '({:start 0 :end 6  :match "apache" "name" "apache"})                                          (re-seq apache-re      "apache"                                          ncgs)))
      (is (= '({:start 0 :end 10 :match "Apache 2.0" "name" "Apache" "version" "2.0"})                      (re-seq apache-re      "Apache 2.0"                                      ncgs)))
      (is (= '({:start 0 :end 8  :match "Apache 1" "name" "Apache" "version" "1"})                          (re-seq apache-re      "Apache 1"                                        ncgs)))
      (is (= '({:start 0 :end 33 :match "Apache Software License Version 2" "name" "Apache" "version" "2"}) (re-seq apache-re      "Apache Software License Version 2"               ncgs)))
      (is (= '({:start 7 :end 40 :match "Apache Software License Version 2" "name" "Apache" "version" "2"}) (re-seq apache-re      "prefix Apache Software License Version 2 suffix" ncgs))))))

