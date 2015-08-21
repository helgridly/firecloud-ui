(ns org.broadinstitute.firecloud-ui.page.workspace.details
  (:require
    [dmohs.react :as react]
    [org.broadinstitute.firecloud-ui.common.components :as comps]
    [org.broadinstitute.firecloud-ui.common.style :as style]
    [org.broadinstitute.firecloud-ui.page.workspace.summary-tab :as summary-tab]
    [org.broadinstitute.firecloud-ui.page.workspace.data-tab :as data-tab]
    [org.broadinstitute.firecloud-ui.page.workspace.method-configs-tab :as method-configs-tab]
    [org.broadinstitute.firecloud-ui.page.workspace.submissions :refer [render-submissions]]
    [org.broadinstitute.firecloud-ui.paths :as paths]
    [org.broadinstitute.firecloud-ui.utils :as utils]
    ))


(react/defc WorkspaceDetails
  {:render
   (fn [{:keys [props]}]
     [:div {}
      [comps/TabBar {:key "selected"
                     :items [{:text "Summary"
                              :component (summary-tab/render (:workspace-id props))}
                             {:text "Data" :component (data-tab/render (:workspace-id props))}
                             {:text "Method Configurations"
                              :component (method-configs-tab/render (:workspace-id props))}
                             {:text "Monitor"
                              :component (render-submissions ws)}
                             {:text "Files"}]}]])})


(defn render-workspace-details [workspace-id]
  (react/create-element WorkspaceDetails {:workspace-id workspace-id}))
