(ns anathema-re.client
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]))

(js/console.log "Does this reload?")


(defn mount-it []
      (rum/mount (ui/app-core (.. js/window -location -pathname)) (.getElementById js/document "appmount")))

(mount-it)