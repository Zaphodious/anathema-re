(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [anathema-re.auth-flow :as aaf]))

(js/console.log "Does this reload?")

(defn init-client [path]
  (set! (.-onGoogleYoloLoad js/window)
        (fn [a] (aaf/init-auth a)))
  (dl/init-app-state
    #(rum/mount (ui/app-core {:path                  path
                              :get-thing             dl/get-under-path
                              :put-thing!            (fn [a] a)
                              :reactive-atom         dl/page-temp-state
                              :current-user-atom     dl/auth-cache
                              :user-info-get         dl/put-under-path-and-mark-changed!
                              :auth-response-handler aaf/transform-auth-response
                              :api-key               js/sitekey})
                (.getElementById js/document "appmount"))
    path))

(init-client (data/get-path-from-uri (.. js/window -location -pathname)))
