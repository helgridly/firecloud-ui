(ns org.broadinstitute.firecloud-ui.page.workspace.summary.tab
  (:require
    [dmohs.react :as react]
    [org.broadinstitute.firecloud-ui.common :as common]
    [org.broadinstitute.firecloud-ui.common.components :as comps]
    [org.broadinstitute.firecloud-ui.common.dialog :as dialog]
    [org.broadinstitute.firecloud-ui.common.icons :as icons]
    [org.broadinstitute.firecloud-ui.common.style :as style]
    [org.broadinstitute.firecloud-ui.endpoints :as endpoints]
    [org.broadinstitute.firecloud-ui.nav :as nav]
    [org.broadinstitute.firecloud-ui.page.workspace.monitor.common :refer [all-success?]]
    [org.broadinstitute.firecloud-ui.page.workspace.summary.acl-editor :refer [AclEditor]]
    [org.broadinstitute.firecloud-ui.page.workspace.summary.attribute-editor :as attributes]
    [org.broadinstitute.firecloud-ui.page.workspace.summary.workspace-cloner :refer [WorkspaceCloner]]
    ))


(defn- render-tags [tags]
  (let [tagstyle {:marginRight 13 :borderRadius 2 :padding "5px 13px"
                  :backgroundColor (:tag-background style/colors)
                  :color (:tag-foreground style/colors)
                  :display "inline-block" :fontSize "94%"}]
    [:div {}
     (map (fn [tag] [:span {:style tagstyle} tag]) tags)]))


(react/defc DeleteDialog
  {:render
   (fn [{:keys [state props this]}]
     [dialog/Dialog
      {:width 500 :dismiss-self (:dismiss-self props)
       :content
       (react/create-element
         [:div {}
          (when (:deleting? @state)
            [comps/Blocker {:banner "Deleting..."}])
          [dialog/OKCancelForm
           {:dismiss-self (:dismiss-self props) :header "Confirm Delete"
            :content
            [:div {}
             [:p {:style {:margin 0}} "Are you sure you want to delete this workspace?"]
             [:p {} "Bucket data will be deleted too."]
             [comps/ErrorViewer {:error (:server-error @state)}]]
            :ok-button [comps/Button {:text "Delete" :onClick #(react/call :delete this)}]}]])}])
   :component-did-mount
   (fn []
     (common/scroll-to-top 100))
   :delete
   (fn [{:keys [props state]}]
     (swap! state assoc :deleting? true :server-error nil)
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/delete-workspace (:workspace-id props))
        :on-done (fn [{:keys [success? get-parsed-response]}]
                   (swap! state dissoc :deleting?)
                   (if success?
                     ((:on-delete props))
                     (swap! state assoc :server-error (get-parsed-response))))}))})


(defn- render-overlays [state props billing-projects]
  [:div {}
   (when (:show-delete-dialog? @state)
     [DeleteDialog
      {:dismiss-self #(swap! state dissoc :show-delete-dialog?)
       :workspace-id (:workspace-id props)
       :on-delete (:on-delete props)}])
   (when (:deleting-attrs? @state)
     [comps/Blocker {:banner "Deleting Attributes..."}])
   (when (:updating-attrs? @state)
     [comps/Blocker {:banner "Updating Attributes..."}])
   (when (contains? @state :locking?)
     [comps/Blocker {:banner (if (:locking? @state) "Unlocking..." "Locking...")}])
   (when (:editing-acl? @state)
     [AclEditor {:workspace-id (:workspace-id props)
                 :dismiss-self #(swap! state dissoc :editing-acl?)
                 :update-owners #(swap! state update-in [:server-response :workspace] assoc "owners" %)}])
   (when (:cloning? @state)
     [WorkspaceCloner {:dismiss #(swap! state dissoc :cloning?)
                       :on-success (fn [namespace name]
                                     (swap! state dissoc :cloning?)
                                     ((:on-clone props) (str namespace ":" name)))
                       :workspace-id (:workspace-id props)
                       :billing-projects billing-projects}])])


(defn- render-sidebar [state props refs this ws billing-projects owner? writer?]
  (let [locked? (get-in ws ["workspace" "isLocked"])
        status (common/compute-status ws)]
    [:div {:style {:flex "0 0 270px" :paddingRight 30}}
     [comps/StatusLabel {:text (str status
                                 (when (= status "Running")
                                   (str " (" (get-in ws ["workspaceSubmissionStats" "runningSubmissionsCount"]) ")")))
                         :icon (case status
                                 "Complete" [icons/CompleteIcon {:size 36}]
                                 "Running" [icons/RunningIcon {:size 36}]
                                 "Exception" [icons/ExceptionIcon {:size 36}])
                         :color (style/color-for-status status)}]
     (when (or owner? writer?)
       (if (not (:editing? @state))
         [comps/SidebarButton
          {:style :light :color :button-blue :margin :top
           :text "Edit" :icon :pencil
           :onClick #(swap! state assoc
                       :reserved-keys (vec (range 0 (count (:attrs-list @state))))
                       :orig-attrs (:attrs-list @state) :editing? true)}]
         [:div {}
          [comps/SidebarButton
           {:style :light :color :button-blue :margin :top
            :text "Save" :icon :document
            :onClick #(attributes/save-attributes state props this
                        (common/get-text refs "descriptionArea"))}]
          [comps/SidebarButton
           {:style :light :color :exception-red :margin :top
            :text "Cancel Editing" :icon :x
            :onClick #(swap! state assoc
                        :editing? false
                        :attrs-list (:orig-attrs @state))}]]))
     (when-not (:editing? @state)
       [comps/SidebarButton {:style :light :margin :top :color :button-blue
                             :text "Clone..." :icon :plus
                             :disabled? (when (empty? billing-projects) "No Google projects available")
                             :onClick #(swap! state assoc :cloning? true)}])
     (when-not (and owner? (:editing? @state))
       [comps/SidebarButton {:style :light :margin :top :color :button-blue
                             :text (if locked? "Unlock" "Lock") :icon :locked
                             :onClick #(react/call :lock-or-unlock this locked?)}])
     (when-not (and owner? (:editing? @state))
       [comps/SidebarButton {:style :light :margin :top :color :exception-red
                             :text "Delete" :icon :trash-can
                             :disabled? (if locked? "This workspace is locked")
                             :onClick #(swap! state assoc :show-delete-dialog? true)}])]))


(defn- render-main [state refs ws owner? submissions]
  (let [owners (ws "owners")]
    [:div {:style {:flex "1 1 auto" :display "flex"}}
     [:div {:style {:flex "1 1 50%"}}
      (style/create-section-header (str "Workspace Owner" (when (> (count owners) 1) "s")))
      (style/create-paragraph
        [:div {}
         (interpose ", " owners)
         (when owner?
           [:span {}
            " ("
            (style/create-link {:text "Sharing..."
                                :onClick #(swap! state assoc :editing-acl? true)})
            ")"])])
      (style/create-section-header "Description")
      (style/create-paragraph
        (let [description (not-empty (get-in ws ["workspace" "attributes" "description"]))]
          (cond (:editing? @state) (react/create-element
                                     (style/create-text-area {:ref "descriptionArea"
                                                              :defaultValue description
                                                              :style {:width 400}
                                                              :rows 5}))
                description description
                :else [:span {:style {:fontStyle "oblique"}} "No description provided"])))
      (attributes/view-attributes state refs)]
     [:div {:style {:flex "1 1 50%" :paddingLeft 10}}
      (style/create-section-header "Created By")
      (style/create-paragraph
        [:div {} (get-in ws ["workspace" "createdBy"])]
        [:div {} (common/format-date (get-in ws ["workspace" "createdDate"]))])
      (style/create-section-header "Google Bucket")
      (style/create-paragraph
        [:a {:href (str "https://console.developers.google.com/storage/browser/" (get-in ws ["workspace" "bucketName"]) "/")
              :title "Click to open the Google Cloud Storage browser for this bucket"
              :style {:textDecoration "none" :color (:button-blue style/colors)}
              :target "_blank"}
         (get-in ws ["workspace" "bucketName"])])
      (style/create-section-header "Analysis Submissions")
      (style/create-paragraph
        (let [fail-count (->> submissions
                           (filter (complement all-success?))
                           count)]
          (str (count submissions) " Submissions"
            (when (pos? fail-count)
              (str " (" fail-count " failed)")))))]]))


(react/defc Summary
  {:refresh
   (fn [{:keys [state this]}]
     (swap! state dissoc :server-response :submission-response)
     (react/call :load-workspace this))
   :render
   (fn [{:keys [refs state props this]}]
     (let [server-response (:server-response @state)
           {:keys [workspace submissions billing-projects server-error]} server-response]
       (cond
         server-error
         (style/create-server-error-message server-error)
         (some nil? [workspace submissions billing-projects])
         [:div {:style {:textAlign "center" :padding "1em"}}
          [comps/Spinner {:text "Loading workspace..."}]]
         :else
         (let [owner? (= "OWNER" (workspace "accessLevel"))
               writer? (or owner? (= "WRITER" (workspace "accessLevel")))]
           [:div {:style {:margin "45px 25px" :display "flex"}}
            (render-overlays state props billing-projects)
            (render-sidebar state props refs this workspace billing-projects owner? writer?)
            (render-main state refs workspace owner? submissions)]))))
   :load-workspace
   (fn [{:keys [props state]}]
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/get-workspace (:workspace-id props))
        :on-done (fn [{:keys [success? get-parsed-response status-text]}]
                   (if success?
                     (let [workspace (get-parsed-response)
                           attributes (get-in workspace ["workspace" "attributes"])]
                       (swap! state update-in [:server-response]
                         assoc :workspace workspace)
                       (swap! state assoc :attrs-list (mapv (fn [[k v]] [k v])
                                                        (dissoc attributes "description"))))
                     (swap! state update-in [:server-response]
                       assoc :server-error status-text)))})
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/list-submissions (:workspace-id props))
        :on-done (fn [{:keys [success? status-text get-parsed-response]}]
                   (if success?
                     (swap! state update-in [:server-response]
                       assoc :submissions (get-parsed-response))
                     (swap! state update-in [:server-response]
                       assoc :server-error status-text)))})
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/get-billing-projects)
        :on-done (fn [{:keys [success? status-text get-parsed-response]}]
                   (if success?
                     (swap! state update-in [:server-response]
                       assoc :billing-projects (get-parsed-response))
                     (swap! state update-in [:server-response]
                       assoc :server-error status-text)))}))
   :lock-or-unlock
   (fn [{:keys [props state this]} locked-now?]
     (swap! state assoc :locking? locked-now?)
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/lock-or-unlock-workspace (:workspace-id props) locked-now?)
        :on-done (fn [{:keys [success? status-text status-code]}]
                   (when-not success?
                     (if (and (= status-code 409) (not locked-now?))
                       (js/alert "Could not lock workspace, one or more analyses are currently running")
                       (js/alert (str "Error: " status-text))))
                   (swap! state dissoc :locking?)
                   (react/call :refresh this))}))
   :component-did-mount
   (fn [{:keys [this]}]
     (react/call :load-workspace this))})
