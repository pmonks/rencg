;
; Copyright © 2024 Peter Monks
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

(in-ns 'rencg.api)

(defmulti re-named-groups
  "Returns the names of all of the named-capturing groups in the
  given regular expression (or matcher) as a set of Strings, or an empty
  set if there aren't any.

  Note: on older JDKs (pre v20), this uses a JDK-agnostic workaround for
  https://bugs.openjdk.org/browse/JDK-7032377"
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
