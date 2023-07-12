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
  "Returns the names of all of the named-capturing groups in the
  given regular expression as a set.

  Note: JDK-agnostic workaround for https://bugs.openjdk.org/browse/JDK-7032377
  (which is fixed in JDK 20)"
  [re]
  (when re (set (map second (re-seq #"\(\?<([a-zA-Z][a-zA-Z0-9]*)>" (str re))))))


(defn re-matches-ncg
  "Equivalent to clojure.core/re-matches, but instead of returning
  the match and sequence of groups, returns a (potentially empty)
  map of just the named-capturing groups in the regex if there was
  a match, or nil if the regex didn't match. Each key in the map is
  the String value of the name of that named-capturing group, and
  the corresponding value is a String containing the text that
  matched that group.

  If the regex is being reused many times, the 3-arg version will
  be more efficient as it allows the caller to calculate the
  named-capturing groups in the regex once, then reuse that
  information, avoiding re-parsing of the regex on each call."
  ([re s] (re-matches-ncg re s nil))
  ([re s ncgs]
   (let [matcher (re-matcher re s)]
     (when (.matches matcher)
       (let [ncgs (or ncgs (re-named-groups re))]
         (loop [result {}
                f      (first ncgs)
                r      (rest ncgs)]
           (if f
             (let [v (try (.group matcher ^String f) (catch java.lang.IllegalArgumentException _ nil))]
               (recur (merge result (when v {f v}))
                      (first r)
                      (rest r)))
             result)))))))
