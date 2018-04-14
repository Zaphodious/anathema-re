(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]))

(js/console.log "Does this reload?")


(defn mount-it []
      (rum/mount (ui/app-core {:path          (data/get-path-from-uri (.. js/window -location -pathname))
                               :get-thing     dl/get-under-path
                               :put-thing!    (fn [a] a)
                               :reactive-atom dl/page-temp-state
                               :current-user  ""})
                 (.getElementById js/document "appmount")))

(mount-it)