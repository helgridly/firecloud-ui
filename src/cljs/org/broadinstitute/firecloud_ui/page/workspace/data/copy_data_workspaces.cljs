(ns org.broadinstitute.firecloud-ui.page.workspace.data.copy-data-workspaces
  (:require
    [clojure.string]
    [dmohs.react :as react]
    [clojure.set :refer [union]]
    [org.broadinstitute.firecloud-ui.common.components :as comps]
    [org.broadinstitute.firecloud-ui.common.table :as table]
    [org.broadinstitute.firecloud-ui.endpoints :as endpoints]
    [org.broadinstitute.firecloud-ui.common.style :as style]
    [org.broadinstitute.firecloud-ui.page.workspace.data.copy-data-entities :as copy-data-entities]
    [org.broadinstitute.firecloud-ui.utils :as utils]
    ))

(react/defc WorkspaceList
  {:render
   (fn [{:keys [props]}]
     [:div {:style {:margin "1em"}}
      (let [attribute-keys (apply union (map (fn [e] (set (keys (e "attributes")))) (:workspaces props)))]
        [table/Table
         {:empty-message "There are no workspaces to display."
          :columns (concat
                     [{:header "Google Project" :starting-width 150}
                      {:header "Name" :starting-width 150
                       :as-text #(get-in % ["workspace" "name"]) :sort-by :text
                       :content-renderer
                       (fn [ws]
                         (style/create-link {:text (get-in ws ["workspace" "name"])
                                             :onClick #((:onWorkspaceSelected props) ws)}))}
                      {:header "Created By" :starting-width 200}
                      (table/date-column {})
                      {:header "Access Level" :starting-width 100}]
                     (map (fn [k] {:header k :starting-width 100}) attribute-keys))
          :data (:workspaces props)
          :->row (fn [ws]
                   (concat
                    [(get-in ws ["workspace" "namespace"])
                     ws
                     (get-in ws ["workspace" "createdBy"])
                     (get-in ws ["workspace" "createdDate"])
                     (ws "accessLevel")]
                    (map (fn [k] (get-in ws ["attributes" k])) attribute-keys)))}])])})

(defn- remove-self [workspace-list workspace-id]
  (filter #(not= workspace-id {:namespace (get-in % ["workspace" "namespace"])
                               :name (get-in % ["workspace" "name"])}) workspace-list))

(react/defc Page
  {:render
   (fn [{:keys [state props]}]
     (let [selected-workspace (:selected-workspace (first (:crumbs props)))]
       (cond
         selected-workspace
         [copy-data-entities/Page {:ref "data-import"
                                   :workspace-id (:workspace-id props)
                                   :selected-from-workspace selected-workspace
                                   :reload-data-tab (:reload-data-tab props)}]
         (:workspaces @state)
         [WorkspaceList {:workspaces (:workspaces @state)
                         :onWorkspaceSelected
                         (fn [ws]
                           ((:add-crumb props)
                            {:text (str (get-in ws ["workspace" "namespace"]) "/"
                                        (get-in ws ["workspace" "name"]))
                             :selected-workspace ws}))}]
         (:error-message @state) (style/create-server-error-message (:error-message @state))
         :else [:div {:style {:textAlign "center"}}
                [comps/Spinner {:text "Loading workspaces..."}]])))
   :component-did-mount
   (fn [{:keys [state props]}]
     (endpoints/call-ajax-orch
       {:endpoint endpoints/list-workspaces
        :on-done (fn [{:keys [success? status-text get-parsed-response]}]
                   (if success?
                     (swap! state assoc :workspaces
                            (remove-self (get-parsed-response) (:workspace-id props)))
                     (swap! state assoc :error-message status-text)))}))})
