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

(defmulti re-named-groups
  "Returns the names of all of the named-capturing groups in the
  given regular expression (or matcher), as a set of Strings.

  Note: JDK-agnostic workaround for https://bugs.openjdk.org/browse/JDK-7032377
  (which is fixed in JDK 20)"
  {:arglists '([re] [m])}
  (fn [arg] (type arg)))

(defmethod re-named-groups nil
  [_]
  nil)

(defmethod re-named-groups java.util.regex.Pattern
  [^java.util.regex.Pattern re]
  (set (map second (re-seq #"\(\?<([a-zA-Z][a-zA-Z0-9]*)>" (str re)))))

(defmethod re-named-groups java.util.regex.Matcher
  [^java.util.regex.Matcher m]
  (re-named-groups (.pattern m)))

(defn re-groups-ncg
  "Equivalent to clojure.core/re-groups, but instead of returning
  a sequence containing the entire match and each group, it returns
  a (potentially empty) map of just the named-capturing groups. Each
  key in the map is the String value of the name of that named-
  capturing group, and the corresponding value is a String containing
  the text that matched that group."
  ([^java.util.regex.Matcher m] (re-groups-ncg m nil))
  ([^java.util.regex.Matcher m ncgs]
   (let [ncgs (or ncgs (re-named-groups m))]
     (loop [result {}
            f      (first ncgs)
            r      (rest ncgs)]
       (if f
         (let [v (try (.group m ^String f) (catch java.lang.IllegalArgumentException _ nil))]
           (recur (merge result (when v {f v}))
                  (first r)
                  (rest r)))
         result)))))

(defn re-matches-ncg
  "Equivalent to clojure.core/re-matches, but returns the result
  of calling re-groups-ncg when there's a match, or nil otherwise.

  If the regex is being reused many times, the 3-arg version will
  be more efficient as it allows the caller to calculate the
  named-capturing groups in the regex once, then reuse that
  information, avoiding re-parsing of the regex on each call."
  ([^java.util.regex.Pattern re s] (re-matches-ncg re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [m (re-matcher re s)]
     (when (.matches m)
       (re-groups-ncg m ncgs)))))

(defmulti re-find-ncg
  "Equivalent to clojure.core/re-find, but returns the result
  of calling re-groups-ncg when the pattern is found, or nil
  otherwise.

  If multiple finds are being performed, the versions where
  the sequence of named-capturing groups is passed in will be
  more efficient as they allow the caller to calculate the
  named-capturing groups in the regex once, then reuse that
  information, avoiding re-parsing of the regex on each call."
  {:arglists '([m] [m ncgs] [re s] [re s ncgs])}
  (fn [f & _] (type f)))

(defmethod re-find-ncg nil
  [& _]
  (re-find nil))  ; This call to re-find may seem bogus, however it ensures we throw _exactly_ the same exception that it throws when passed nil

(defmethod re-find-ncg java.util.regex.Matcher
  ([^java.util.regex.Matcher m] (re-find-ncg m nil))
  ([^java.util.regex.Matcher m ncgs]
   (when (.find m)
     (re-groups-ncg m ncgs))))

(defmethod re-find-ncg java.util.regex.Pattern
  ([^java.util.regex.Pattern re s] (re-find-ncg re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [m (re-matcher re s)]
     (re-find-ncg m ncgs))))

(defn re-seq-ncg
  "Equivalent to clojure.core/re-seq, but returns the result
  of calling re-groups-ncg on each successive match, or nil
  if there are no matches.

  If the regex is being reused many times, the 3-arg version
  will be more efficient as it allows the caller to calculate
  the named-capturing groups in the regex once, then reuse that
  information, avoiding re-parsing of the regex on each call."
  ([^java.util.regex.Pattern re s] (re-seq-ncg re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [ncgs (or ncgs (re-named-groups re))
         m    (re-matcher re s)]
     ((fn step []
        (when (.find m)
          (cons (re-groups-ncg m ncgs) (lazy-seq (step)))))))))
