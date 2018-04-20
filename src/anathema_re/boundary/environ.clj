(ns anathema-re.boundary.environ
  (:require
    [integrant.core :as ig]
    [environ.core :refer [env]]
    [cemerick.url :as url]))

(defmethod ig/init-key :anathema-re.boundary/environ [_ {:keys [imgur-id
                                                                imgur-secret]
                                                         :as options}]
  (println "resetting environ")
  {:imgur-id (or (env :imgur-id) imgur-id)
   :imgur-secret (or (env :imgur-secret) imgur-secret)
   :mongodb-uri (url/url-decode (env :mongodb-uri ""))
   :masterkey (env :masterkey)
   :goog-api (env :goog-api)})
