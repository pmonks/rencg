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
            [rencg.api    :refer [re-named-groups re-matches-ncg]]))

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
    (is (= {} (re-matches-ncg #".*"             "")))
    (is (= {} (re-matches-ncg #"foo"            "foo")))
    (is (= {} (re-matches-ncg #"(?<foo>foo)?.*" "bar"))))
  (testing "Matches that do have named-capturing groups"
    (is (= {"foo" "foo"}                      (re-matches-ncg #"(?<foo>foo)"    "foo")))
    (is (= {"content" "foobar"}               (re-matches-ncg #"(?<content>.*)" "foobar")))
    (is (= {"name" "Apache"}                  (re-matches-ncg apache-re "Apache")))
    (is (= {"name" "apache"}                  (re-matches-ncg apache-re "apache")))
    (is (= {"name" "Apache", "version" "2.0"} (re-matches-ncg apache-re "Apache 2.0")))
    (is (= {"name" "Apache", "version" "1"}   (re-matches-ncg apache-re "Apache 1")))
    (is (= {"name" "Apache", "version" "2"}   (re-matches-ncg apache-re "Apache Software License Version 2")))))
