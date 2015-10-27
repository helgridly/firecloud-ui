(ns org.broadinstitute.firecloud-ui.page.method-repo
  (:require
   [dmohs.react :as react]
   [org.broadinstitute.firecloud-ui.common :as common]
   [org.broadinstitute.firecloud-ui.common.components :as comps]
   [org.broadinstitute.firecloud-ui.utils :as utils]
   [org.broadinstitute.firecloud-ui.page.method-repo-ns-acls :as nsacls]
   [org.broadinstitute.firecloud-ui.page.method-config-importer :refer [MethodConfigImporter]]
   ))


(react/defc Page
  {:render
   (fn [{:keys [state]}]
     [:div {:style {:padding "1em"}}
      [:h2 {} "Method Repository"]
      (when
        (or
          (not (nil? (:methods @state)))
          (not (nil? (:configs @state))))
        [:div {:style {:float "right"}}
         [comps/Button
          {:text "Namespace Permissions..."
           :onClick #(swap! state assoc :show-ns-acl true)
           ;#(js/alert "you clicked it!")


           }]])
      (common/clear-both)
      (when (:show-ns-acl @state)

         (let [get-ns-func (fn [e] (get e "namespace"))]


         [nsacls/AgoraNSPermsEditor
         {:dismiss-self #(swap! state dissoc :show-ns-acl)
          :methods-ns-list (distinct (map get-ns-func (:methods @state)))
          :configs-ns-list (distinct (map get-ns-func (:configs @state)))



          }

         ])
        )
      [MethodConfigImporter {:on-load-methods (fn [methods]
                                                (utils/rlog (str "setting methods"))
                                                (swap! state assoc :methods methods))
                             :on-load-configs (fn [configs]
                                                (swap! state assoc :configs configs))}]])})
