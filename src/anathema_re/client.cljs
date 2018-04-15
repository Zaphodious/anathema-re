(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]))

(js/console.log "Does this reload?")


(defn init-client [path]
  (dl/init-app-state
    #(rum/mount (ui/app-core {:path          path
                              :get-thing     dl/get-under-path
                              :put-thing!    (fn [a] a)
                              :reactive-atom dl/page-temp-state
                              :current-user  ""
                              :entity (dl/get-under-path path)})
                (.getElementById js/document "appmount"))
    path))

(init-client (data/get-path-from-uri (.. js/window -location -pathname)))
