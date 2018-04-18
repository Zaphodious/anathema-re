(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [anathema-re.auth-flow :as aaf]))

(js/console.log "Does this reload?")

(defn yo-load [g-yolo]
  (println "Starting YOLO Service")
  (let [auth-obj {:supportedAuthMethods      ["https://accounts.google.com"]
                  :supportedIdTokenProviders [{:uri      "https://accounts.google.com"
                                               :clientId js/sitekey}]}
        retrieve-promise (.retrieve g-yolo (clj->js auth-obj))
        hint-promise (-> g-yolo (.hint (clj->js (assoc auth-obj :context "signUp")))
                         (.then (fn [a] (println "token is " (.-idToken a)))))]

    (-> retrieve-promise
        (.catch (fn [a] (println "sign-in failed, " a) a))
        (.catch (fn [a] (.hint g-yolo (clj->js (assoc auth-obj :context "signUp")))))
        (.then dl/handle-credential
               (fn [error] (println "Credential error is " (js->clj error)))))))

(defn init-client [path]
  (set! (.-onGoogleYoloLoad js/window)
        (fn [a] (aaf/init-auth a)))
  (dl/init-app-state
    #(rum/mount (ui/app-core {:path          path
                              :get-thing     dl/get-under-path
                              :put-thing!    (fn [a] a)
                              :reactive-atom dl/page-temp-state
                              :current-user  ""
                              :user-info-get (fn [] nil)
                              :api-key js/sitekey
                              :entity (dl/get-under-path path)})
                (.getElementById js/document "appmount"))
    path))

(init-client (data/get-path-from-uri (.. js/window -location -pathname)))
