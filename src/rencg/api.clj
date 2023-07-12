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

(ns rencg.api)

(defn re-named-groups
  "Returns a sequence of the names of all of the named-capturing
  groups in the given regular expression.
  Note: JDK-agnostic workaround for https://bugs.openjdk.org/browse/JDK-7032377
  (which is fixed in JDK 20)"
  [re]
  (when re (map second (re-seq #"\(\?<([a-zA-Z][a-zA-Z0-9]*)>" (str re)))))


(defn re-matches-ncg
  "Returns the match, if any, of string to pattern, using
  java.util.regex.Matcher.matches(). Returns a (potentially
  empty) map of the named-capturing groups in the regex if there
  was a match, or nil otherwise. Each key in the map is the name
  of a name-capturing group, and each value is the corresponding
  value in the string that matched that group."
  [re s]
  (let [matcher (re-matcher re s)]
    (when (.matches matcher)
      (let [ncgs (re-named-groups re)]
        (loop [result {}
               f      (first ncgs)
               r      (rest ncgs)]
          (if f
            (let [v (try (.group matcher ^String f) (catch java.lang.IllegalArgumentException _ nil))]
              (recur (merge result (when v {f v}))
                     (first r)
                     (rest r)))
            result))))))
