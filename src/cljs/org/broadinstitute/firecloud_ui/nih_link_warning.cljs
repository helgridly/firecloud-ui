(ns org.broadinstitute.firecloud-ui.nih-link-warning
  (:require
   [dmohs.react :as react]
   [org.broadinstitute.firecloud-ui.common :as common]
   [org.broadinstitute.firecloud-ui.page.profile :as profile]
   [org.broadinstitute.firecloud-ui.utils :as utils]
   )
  (:require-macros [devcards.core :refer [defcard]]))


(defn- get-profile [ajax-args on-done]
  (let [on-done (if (or (nil? (:canned-response ajax-args)) (:use-canned-response? ajax-args))
                  on-done
                  (fn [{:keys [xhr] :as m}]
                    (utils/cljslog {:status (.-status xhr)
                                    :statusText (.-statusText xhr)
                                    :responseText (.-responseText xhr)})
                    (on-done m)))]
    (utils/ajax-orch
     "/profile"
     (merge ajax-args {:on-done on-done})
     :service-prefix "/service/register")))


(react/defc NihLinkWarning
  {:render
   (fn [{:keys [props state]}]
     (when-let [profile (:profile @state)]
       (let [expire-time (-> (:linkExpireTime profile) js/parseInt (* 1000) (js/moment.))
             _24-hours-from-now (.add (js/moment.) 24 "hours")]
         (when (and (= (:isDbgapAuthorized profile) "true")
                    (.isBefore expire-time _24-hours-from-now))
           [:div {:style {:border "1px solid #c00" :backgroundColor "#fcc"
                          :color "#800" :fontSize "small" :padding "6px 10px"}}
            "Your NIH account link (" (:linkedNihUsername profile) ") will expire "
            (.calendar expire-time) ". "
            [:a {:href (profile/get-nih-link-href)} "Re-link"]
            " your account before then to retain dbGAP authorization."]))))
   :component-did-mount
   (fn [{:keys [props state]}]
     (get-profile
      (:ajax-args props)
      (fn [{:keys [success? get-parsed-response]}]
        ;; Silently fail on errors?
        (when success?
          (swap! state assoc :profile (common/parse-profile (get-parsed-response)))))))})


(defcard not-authorized
  "This should show **nothing**."
  (react/wrap-devcard-fn
   (fn [data-atom owner devcard-props]
     [NihLinkWarning
      (merge
       devcard-props
       {:ajax-args
        {:use-canned-response? true
         :canned-response
         {:delay-ms 2000
          :status 200,
          :statusText "OK",
          :responseText
          "{\n  \"userId\": \"103758681029835863513\",\n  \"keyValuePairs\": [{\n    \"key\": \"email\",\n    \"value\": \"dmohs@broadinstitute.org\"\n  }, {\n    \"key\": \"institution\",\n    \"value\": \"Broad\"\n  }, {\n    \"key\": \"isRegistrationComplete\",\n    \"value\": \"true\"\n  }, {\n    \"key\": \"name\",\n    \"value\": \"David Mohs\"\n  }, {\n    \"key\": \"pi\",\n    \"value\": \"Frank Underwood\"\n  }]\n}"}}})]))
  nil
  {:inspect-data true})


(defcard not-expiring-soon
  "This should show **nothing**."
  (react/wrap-devcard-fn
   (fn [data-atom owner devcard-props]
     [NihLinkWarning
      (merge
       devcard-props
       {:ajax-args
        {:use-canned-response? true
         :canned-response
         {:delay-ms 2000
          :status 200,
          :statusText "OK",
          :responseText
          (str "{\n  \"userId\": \"103758681029835863513\",\n  \"keyValuePairs\": [{\n    \"key\": \"email\",\n    \"value\": \"dmohs@broadinstitute.org\"\n  }, {\n    \"key\": \"institution\",\n    \"value\": \"Broad\"\n  }, {\n    \"key\": \"isDbgapAuthorized\",\n    \"value\": \"false\"\n  }, {\n    \"key\": \"isRegistrationComplete\",\n    \"value\": \"true\"\n  }, {\n    \"key\": \"lastLinkTime\",\n    \"value\": \"1451937714\"\n  }, {\n    \"key\": \"linkedNihUsername\",\n    \"value\": \"CBIRGER_SO\"\n  }, {\n    \"key\": \"linkExpireTime\",\n    \"value\": \""
               (.. (js/moment.) (add 20 "days") (unix))
               "\"\n  }, {\n    \"key\": \"name\",\n    \"value\": \"David Mohs\"\n  }, {\n    \"key\": \"pi\",\n    \"value\": \"Frank Underwood\"\n  }]\n}")}}})]))
  nil
  {:inspect-data true})


(defcard expires-soon
  "This should show the warning once the ajax call completes."
  (react/wrap-devcard-fn
   (fn [data-atom owner devcard-props]
     [NihLinkWarning
      (merge
       devcard-props
       {:ajax-args
        {:use-canned-response? true
         :canned-response
         {:delay-ms 2000
          :status 200,
          :statusText "OK",
          :responseText
          (str "{\n  \"userId\": \"103758681029835863513\",\n  \"keyValuePairs\": [{\n    \"key\": \"email\",\n    \"value\": \"dmohs@broadinstitute.org\"\n  }, {\n    \"key\": \"institution\",\n    \"value\": \"Broad\"\n  }, {\n    \"key\": \"isDbgapAuthorized\",\n    \"value\": \"true\"\n  }, {\n    \"key\": \"isRegistrationComplete\",\n    \"value\": \"true\"\n  }, {\n    \"key\": \"lastLinkTime\",\n    \"value\": \"1451937714\"\n  }, {\n    \"key\": \"linkedNihUsername\",\n    \"value\": \"CBIRGER_SO\"\n  }, {\n    \"key\": \"linkExpireTime\",\n    \"value\": \""
               (.. (js/moment.) (add 10 "hours") (unix))
               "\"\n  }, {\n    \"key\": \"name\",\n    \"value\": \"David Mohs\"\n  }, {\n    \"key\": \"pi\",\n    \"value\": \"Frank Underwood\"\n  }]\n}")}}})]))
  nil
  {:inspect-data true})
