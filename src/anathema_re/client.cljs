(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [hireling.core :as hireling]))

(js/console.log "Does this reload?")

(hireling/register-service-worker "/sw.js")

(defn init-client [path]
  (dl/init-app-state
    #(rum/mount (ui/app-core {:path                  (if (empty? path) [:player :me] path)
                              :get-thing             dl/get-under-path
                              :put-thing!            dl/put-under-path-and-mark-changed!
                              :put-image!            dl/put-image-under-path!
                              :reactive-atom         dl/page-temp-state
                              :current-user-atom     dl/auth-cache
                              :api-key               js/sitekey})
                (.getElementById js/document "appmount"))
    path))
(def pathname (.. js/window -location -pathname))
(init-client (data/get-path-from-uri (if (= "/" pathname) "/home/" pathname)))
