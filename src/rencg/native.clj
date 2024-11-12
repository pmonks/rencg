;
; Copyright © 2024 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(in-ns 'rencg.api)

#_{:clj-kondo/ignore [:unresolved-symbol]}
(defmethod re-named-groups java.util.regex.Pattern
  [^java.util.regex.Pattern re]
  (set (keys (.namedGroups re))))
