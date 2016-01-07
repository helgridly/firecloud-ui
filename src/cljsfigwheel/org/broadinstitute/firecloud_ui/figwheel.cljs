(ns ^:figwheel-always org.broadinstitute.firecloud-ui.figwheel
  (:require
   org.broadinstitute.firecloud-ui.dev
   [org.broadinstitute.firecloud-ui.main :as main]
   ))


(main/render-application true)
