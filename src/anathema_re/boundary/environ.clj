(ns anathema-re.boundary.environ
  (:require
    [integrant.core :as ig]
    [environ.core :refer [env]]
    [cemerick.url :as url]))

(defmethod ig/init-key :anathema-re.boundary/environ [_ options]
  (println "resetting environ")
  {:mongodb-uri (url/url-decode (env :mongodb-uri ""))
   :masterkey (env :masterkey)})
