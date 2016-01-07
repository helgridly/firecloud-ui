(ns org.broadinstitute.firecloud-ui.dev
  (:require
   [devtools.core :as devtools]
   ))


;; See https://github.com/binaryage/cljs-devtools
;; With these tools installed and "Enable custom formatters" checked in Chrome's devtools' settings,
;; Clojure data structures are much easier to inspect in the console and debugging windows.
(devtools/set-pref! :install-sanity-hints true)
(devtools/install!)
