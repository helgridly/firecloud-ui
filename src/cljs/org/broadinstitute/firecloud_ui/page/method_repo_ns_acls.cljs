(ns org.broadinstitute.firecloud-ui.page.method-repo-ns-acls
  (:require
    [dmohs.react :as react]
    [clojure.string :refer [trim]]
    [org.broadinstitute.firecloud-ui.common :as common]
    [org.broadinstitute.firecloud-ui.utils :as utils]
    [org.broadinstitute.firecloud-ui.common.components :as comps]
    [org.broadinstitute.firecloud-ui.common.style :as style]
    [org.broadinstitute.firecloud-ui.common.table :as table]
    [org.broadinstitute.firecloud-ui.endpoints :as endpoints]))


(def ^:private access-levels
  ["WRITER" "OWNER"])


(def ^:private column-width "calc(50% - 4px)")


(defn- create-ns-acl-gui [state props this]
  [:div {}
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
           :defaultValue (acl-entry "user")})
        (style/create-select
          {:ref (str "acl-value" i)
           :style {:float "right" :width column-width :height 33}
           :defaultValue (acl-entry "role")}
          access-levels)
        (common/clear-both)])
     (:acl-vec @state))
   [comps/Button
    {:text "Add new" :style :add
     :onClick #(swap! state assoc
                :acl-vec (flatten [(:acl-vec @state)
                                   {"user" "" "role" "WRITER"}]))}]
   [:div {:style {:textAlign "center" :marginTop "1em"}}
    [:a {:href "javascript:;"
         :style {:textDecoration "none"
                 :color (:button-blue style/colors)
                 :marginRight "1.5em"}
         :onClick #((:dismiss-self props))}
     "Cancel"]
    [comps/Button {:text "Save"
                   :onClick #(do
                              (let [ui-state (react/call :capture-ui-state this)]
                                (utils/rlog (str "capture ui-state
                                 that needs to be saved is " ui-state))))}]]])



(react/defc AgoraNSPermsEditor
  {:load-in-acl
   (fn [{:keys [props state this]}]
     (swap! state assoc :blocking-text "Loading Namespace Permissions...")
     (endpoints/call-ajax-orch
       {:endpoint (endpoints/get-agora-ns-acl
                    (= "configs" (:selected-type @state))
                    (:selected-ns @state))
        :on-done (fn [{:keys [success? get-parsed-response status-text]}]
                   (swap! state dissoc :blocking-text)
                   (if success?
                     (do
                       (swap! state dissoc :status-text)
                       (let [parsed-response (get-parsed-response)]
                         (swap! state assoc :acl-vec parsed-response :count-orig (count parsed-response))))
                     (swap! state assoc :status-text status-text)))}))
   :component-did-mount
   (fn [{:keys [props state this]}]
     (when (>= (count (:configs-ns-list props)) 0)
       (swap! state assoc :selected-type "configs"))
     (when (>= (count (:methods-ns-list props)) 0)
       (swap! state assoc :selected-type "methods"))
     (if (= "configs" (:selected-type @state))
       (swap! state assoc :selected-ns-list (:configs-ns-list props))
       (swap! state assoc :selected-ns-list (:methods-ns-list props)))
     (swap! state assoc :selected-ns (first (:selected-ns-list @state)))
     (react/call :load-in-acl this))
   :render
   (fn [{:keys [props state this]}]
     [comps/Dialog
      {:width "75%"
       :blocking? true
       :dismiss-self (:dismiss-self props)
       :content (react/create-element
                  [:div {:style {:background "#fff" :padding "2em"}}
                   (when (:blocking-text @state)
                     [comps/Blocker {:banner (:blocking-text @state)}])
                   [comps/XButton {:dismiss (:dismiss-self props)}]
                   (if
                     (and
                       (== (count (:methods-ns-list props)) 0)
                       (== (count (:configs-ns-list props)) 0))
                     [:div {} "There are no namespaces!"]
                     [:div {}
                      "Setting Namespace permissions for: "
                      (cond (and
                              (> (count (:methods-ns-list props)) 0)
                              (> (count (:configs-ns-list props)) 0))
                            [:div {}
                             [:input {:onClick #(swap! state assoc
                                                 :selected-type "methods"
                                                 :selected-ns-list (:methods-ns-list props))
                                  :checked (== 0 (compare "methods" (:selected-type @state)))
                                  :class "ns-perms" :type "radio"} "Methods"]
                             [:input {:checked (== 0 (compare "configs" (:selected-type @state)))
                                      :onClick #(swap! state assoc
                                                 :selected-type "configs"
                                                 :selected-ns-list (:configs-ns-list props))
                                  :class "ns-perms" :type "radio"} "Configurations"]]
                            (> (count (:methods-ns-list props)) 0) "Methods"
                            (> (count (:configs-ns-list props)) 0) "Configurations")])
                   [:br]
                   "Select a Namespace: "
                   (style/create-select
                     {:onChange (fn [e]
                                  (let [index (int (-> e .-target .-value))
                                        selected-ns-str (nth (:selected-ns-list @state) index)]
                                    (swap! state assoc :selected-ns selected-ns-str)
                                    (react/call :load-in-acl this)))
                      :defaultValue (:selected-ns @state)}
                     (:selected-ns-list @state))
                   (if (:status-text @state)
                     [:div {:style {:color (:exception-red style/colors)}}
                      (:status-text @state)]
                     (create-ns-acl-gui state props this))
                   ])}])
   :capture-ui-state
   (fn [{:keys [state refs]}]
     (mapv
       (fn [i]
         {:user (-> (@refs (str "acl-key" i)) .getDOMNode .-value trim)
          :role (nth access-levels (int (-> (@refs (str "acl-value" i)) .getDOMNode .-value)))})
       (range (count (:acl-vec @state)))))})






