(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [anathema-re.auth-flow :as aaf]
            [hireling.core :as hireling]))

(js/console.log "Does this reload?")

(hireling/register-service-worker "/sw.js")

(defn init-client [path]
  (set! (.-onGoogleYoloLoad js/window)
        (fn [a] (aaf/init-auth a)))
  (dl/init-app-state
    #(rum/mount (ui/app-core {:path                  path
                              :get-thing             dl/get-under-path
                              :put-thing!            dl/put-under-path-and-mark-changed!
                              :reactive-atom         dl/page-temp-state
                              :current-user-atom     dl/auth-cache
                              :auth-response-handler aaf/transform-auth-response
                              :api-key               js/sitekey})
                (.getElementById js/document "appmount"))
    path))

(init-client (data/get-path-from-uri (.. js/window -location -pathname)))
