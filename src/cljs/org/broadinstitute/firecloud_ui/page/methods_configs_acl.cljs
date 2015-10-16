(ns org.broadinstitute.firecloud-ui.page.methods-configs-acl
  (:require
   [dmohs.react :as react]
   [clojure.string :refer [trim]]
   [org.broadinstitute.firecloud-ui.common :as common]
   [org.broadinstitute.firecloud-ui.common.components :as comps]
   [org.broadinstitute.firecloud-ui.common.style :as style]
   [org.broadinstitute.firecloud-ui.common.table :as table]
   [org.broadinstitute.firecloud-ui.endpoints :as endpoints]
   [org.broadinstitute.firecloud-ui.utils :as utils]
   ))


(defn- get-ordered-name [entity]
  (clojure.string/join ":"
    [(entity "namespace")
     (entity "name")
     (entity "snapshotId")]))

(def ^:private access-levels
  ["READER" "OWNER" "NO ACCESS"])

(def ^:private column-width "calc(50% - 4px)")

(defn- filter-public [acl-vec]
  (let [hasNotPublicUser
        (fn [m]
          (utils/rlog (str "m is " m))
          (utils/rlog (str "map? is" (map? m)))
          (utils/rlog (str "contains? is " (contains? m "user")))
          (utils/rlog (str "=public is " (= "public" (m "user"))))
          (not
            (and
              (map? m)
              (contains? m "user")
              (= "public" (m "user")))))]
    (filterv hasNotPublicUser acl-vec)))


(defn- correspondsToReader [access-level]
  (if
    (or
      (= access-level "READER")
      (= access-level "OWNER"))
    true ; if-and-only-if reader or owner then return true
    false))


(defn- extract-last-public-access-level [acl-vec]
  (let [hasPublicUser
        (fn [m]
          (and
            (map? m)
            (contains? m "user")
            (= "public" (m "user"))))
        justPublic (filterv hasPublicUser acl-vec)
        numJustPublic (count justPublic)]
    (if (<= numJustPublic 0)
      ;if public isn't in the acl-return nil
      nil
      (let [lastPublic (get justPublic (- numJustPublic 1))
            lastPublicAccessLevel (get lastPublic "accessLevel")]
        ;if public is in the acl return the value of the last one
        lastPublicAccessLevel))))


(react/defc AgoraPermsEditor
  {:render
   (fn [{:keys [props state this]}]
     [comps/Dialog
      {:width "75%"
       :blocking? true
       :dismiss-self (:dismiss-self props)
       :content (react/create-element
                  [:div {:style {:background "#fff" :padding "2em"}}
                   (cond
                     (:acl-vec @state)
                     [:div {}
                      (when (:saving? @state)
                        [comps/Blocker {:banner "Updating..."}])
                      [:div {:style {:paddingBottom "0.5em" :fontSize "90%"}}
                       [:h4 {} (let [sel-ent (:selected-entity props)
                                     ent-type (sel-ent "entityType")
                                     disp (get-ordered-name sel-ent)]
                                 (str "Permissions for " ent-type " " disp))]
                       [:div {:style
                              {:float "left" :width column-width}}
                        "User or Group ID"]
                       [:div {:style
                              {:float "right" :width column-width}}
                        "Access Level"]
                       (common/clear-both)]
                      (map-indexed
                        (fn [i acl-entry]
                          [:div {}
                           (style/create-text-field
                             {:ref (str "acl-key" i)
                              :style {:float "left" :width column-width
                                      :backgroundColor (when (< i (:count-orig @state))
                                                         (:background-gray style/colors))}
                              :disabled (< i (:count-orig @state))
                              :spellCheck false
                              :onChange (fn [e]
                                          (let [new-val (-> e .-target .-value)
                                                new-val-is-public (= "public" new-val)]
                                            (when new-val-is-public
                                              (do
                                                (js/alert "Cannot set value to 'public'!")
                                                (set! (-> e .-target .-value) "")))))
                              :defaultValue (acl-entry "user")})
                           (style/create-select
                             {:ref (str "acl-value" i)
                              :style {:float "right" :width column-width :height 33}
                              :defaultValue (acl-entry "accessLevel")}
                             access-levels)
                           (common/clear-both)])
                        (:acl-vec @state))
                      [comps/Button
                       {:text "Add new" :style :add
                        :onClick #(swap! state assoc
                                   :acl-vec (flatten [(:acl-vec @state)
                                                      {"user" "" "accessLevel" "READER"}]))}]
                      [:input {:type "checkbox"
                               :checked (:public-status @state)
                               :onChange (fn [e]
                                           (swap! state assoc :public-status
                                             (-> e .-target .-checked)))}]
                      "Publicly Readable?"
                      [:div {:style {:textAlign "center" :marginTop "1em"}}
                       [:a {:href "javascript:;"
                            :style {:textDecoration "none"
                                    :color (:button-blue style/colors)
                                    :marginRight "1.5em"}
                            :onClick #((:dismiss-self props))}
                        "Cancel"]
                       [comps/Button {:text "Save"
                                      :onClick #(react/call :persist-acl this)}]]]
                     (:error @state) (style/create-server-error-message (:error @state))
                     :else [comps/Spinner {:text
                                           (str "Loading Permissions for "
                                             ((:selected-entity props) "entityType") " "
                                             (get-ordered-name (:selected-entity props))
                                             "...")}])])}])
   :component-did-mount
   (fn [{:keys [props state]}]
     (endpoints/call-ajax-orch
       {:endpoint (let [ent (:selected-entity props)
                        name (ent "name")
                        nmsp (ent "namespace")
                        sid (ent "snapshotId")]
                    (endpoints/get-agora-method-acl
                      nmsp name sid (:is-conf props)))
        :on-done (fn [{:keys [success? get-parsed-response status-text]}]
                   (if success?
                     (let [acl-vec (get-parsed-response)
                           last-public-access-level (extract-last-public-access-level acl-vec)
                           new-state-public-status (correspondsToReader last-public-access-level)
                           no-public-acl (filter-public acl-vec)]
                       (swap! state assoc
                         :public-status new-state-public-status
                         :acl-vec no-public-acl
                         :count-orig (count no-public-acl)))
                     (swap! state assoc :error status-text)))}))
   :persist-acl
   (fn [{:keys [props state this]}]
     (swap! state assoc :saving? true)
     (swap! state assoc :acl-vec (react/call :capture-ui-state this))
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/persist-agora-method-acl (:selected-entity props))
        :headers {"Content-Type" "application/json"}
        :payload (filterv #(not (empty? (:user %))) (:acl-vec @state))
        :on-done (fn [{:keys [success? status-text]}]
                   (swap! state dissoc :saving?)
                   (if success?
                     ((:dismiss-self props))
                     (js/alert "Error saving permissions: " status-text)))}))
   :capture-ui-state
   (fn [{:keys [state refs]}]
     (let [captured-ui-fields-vec
           (map
             (fn [i]
               {:user (-> (@refs (str "acl-key" i)) .getDOMNode .-value trim)
                :accessLevel (-> (@refs (str "acl-value" i)) .getDOMNode .-value)})
             (range (count (:acl-vec @state))))
           captured-ui-box-check-status (-> (@refs "publicbox") .getDOMNode .-checked)
           captured-ui-box-map {:user "public" :accessLevel
                                (if captured-ui-box-check-status "READER" "NO ACCESS")}
           captured-ui-fields-and-public-box (flatten [captured-ui-fields-vec
                                               captured-ui-box-map])]
       captured-ui-fields-and-public-box))})