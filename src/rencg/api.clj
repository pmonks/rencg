;
; Copyright © 2023 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns rencg.api
  (:refer-clojure :exclude [re-groups re-matches re-find re-seq]))

(defmulti re-named-groups
  "Returns the names of all of the named-capturing groups in the
  given regular expression (`java.util.regex.Pattern`) or matcher
  (`java.util.regex.Matcher`) as a set of `String`s, or an empty set if there
  aren't any.  Returns `nil` if the argument is `nil`.

  Note: on older JDKs (pre v20), this uses a JDK-agnostic workaround for
  [JDK-7032377](https://bugs.openjdk.org/browse/JDK-7032377)."
  {:arglists '([re] [m])}
  (fn [arg] (type arg)))

(defmethod re-named-groups nil
  [_]
  nil)

(defmethod re-named-groups java.util.regex.Matcher
  [^java.util.regex.Matcher m]
  (re-named-groups (.pattern m)))

; Dynamically load the re-named-groups implementation, based on JVM capabilities
(if (contains? (set (map #(.getName ^java.lang.reflect.Method %) (.getMethods java.util.regex.Pattern))) "namedGroups")
  (load "native")
  (load "non_native"))

(defn re-groups
  "Equivalent to [clojure.core/re-groups](https://clojuredocs.org/clojure.core/re-groups),
  but instead of returning a sequence containing the entire match and each
  group, it returns a map of the named-capturing groups as well as the start
  index (`:start`), end index (`:end`), and text (`:match`) of the entire match.

  The key for each named-capturing group that's found is the (`String`) name of
  that group, and the corresponding value is the (`String`) text that matched
  that group.

  If the same regex is being used many times, the 2-arg version may be more
  efficient as it allows the caller to determine the named-capturing groups in
  the regex once (e.g. using [[re-named-groups]], then reuse that information,
  potentially avoiding re-parsing of the regex on each call."
  ([^java.util.regex.Matcher m] (re-groups m nil))
  ([^java.util.regex.Matcher m ncgs]
   (let [ncgs (seq (or ncgs (re-named-groups m)))]
     (loop [result          {}
            [^String f & r] ncgs]
       (if f
         (let [v (try (.group m f) (catch java.lang.IllegalArgumentException _ nil))]
           (recur (merge result
                         {:start (.start m)
                          :end   (.end   m)
                          :match (.group m)}
                         (when v {f v}))
                  r))
         (merge result
                {:start (.start m)
                 :end   (.end   m)
                 :match (.group m)}))))))

(def ^:deprecated ^:no-doc re-groups-ncg
  "See [[re-groups]]"
  re-groups)

(defn re-matches
  "Equivalent to [clojure.core/re-matches](https://clojuredocs.org/clojure.core/re-matches),
  but returns the result of calling [[re-groups]] when there's a match, or `nil`
  otherwise.

  If the regex is being reused many times, the 3-arg version may be more
  efficient as it allows the caller to determine the named-capturing groups in
  the regex once (e.g. using [[re-named-groups]], then reuse that information,
  potentially avoiding re-parsing of the regex on each call."
  ([^java.util.regex.Pattern re s] (re-matches re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [m (re-matcher re s)]
     (when (.matches m)
       (re-groups m ncgs)))))

(def ^:deprecated ^:no-doc re-matches-ncg
  "See [[re-matches]]"
  re-matches)

(defmulti re-find
  "Equivalent to [clojure.core/re-find](https://clojuredocs.org/clojure.core/re-find),
  but returns the result of calling [[re-groups]] when the pattern is found, or
  `nil` otherwise.

  If multiple finds are being performed, the versions where the sequence of
  named-capturing groups is passed in may be more efficient as they allow the
  caller to determine the named-capturing groups in the regex once (e.g. using
  [[re-named-groups]], then reuse that information, potentially avoiding
  re-parsing of the regex on each call."
  {:arglists '([m] [m ncgs] [re s] [re s ncgs])}
  (fn [f & _] (type f)))

(defmethod re-find nil
  [& _]
  (clojure.core/re-find nil))  ; This call to clojure.core/re-find may seem bogus, however it ensures we throw _exactly_ the same exception that it throws when passed nil

(defmethod re-find java.util.regex.Matcher
  ([^java.util.regex.Matcher m] (re-find m nil))
  ([^java.util.regex.Matcher m ncgs]
   (when (.find m)
     (re-groups m ncgs))))

(defmethod re-find java.util.regex.Pattern
  ([^java.util.regex.Pattern re s] (re-find re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [m (re-matcher re s)]
     (re-find m ncgs))))

(def ^:deprecated ^:no-doc re-find-ncg
  "See [[re-find]]"
  re-find)

(defn re-seq
  "Equivalent to [clojure.core/re-seq](https://clojuredocs.org/clojure.core/re-seq),
  but returns the result of calling [[re-groups]] on each successive match, or
  `nil` if there are no matches.

  If the regex is being reused many times, the 3-arg version may be more
  efficient as it allows the caller to determine the named-capturing groups in
  the regex once (e.g. using [[re-named-groups]], then reuse that information,
  potentially avoiding re-parsing of the regex on each call."
  ([^java.util.regex.Pattern re s] (re-seq re s nil))
  ([^java.util.regex.Pattern re s ncgs]
   (let [ncgs (or ncgs (re-named-groups re))
         m    (re-matcher re s)]
     ((fn step []
        (when (.find m)
          (cons (re-groups m ncgs) (lazy-seq (step)))))))))

(def ^:deprecated ^:no-doc re-seq-ncg
  "See [[re-seq]]"
  re-seq)
