(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]
            [clojure.core.async :as async]))


(defmethod ig/init-key :anathema-re.handler/api [_ {:keys [get-thing put-thing!]
                                                    :as opts}]
  (routes
    (GET "/api/*.:file-ext"
         [file-ext full :as
          {:keys [uri headers query-string]
           :as request}]
      (println "file ext is " file-ext)
      (println "full is " full)
      (println "query is " (pr-str query-string))
      (let [path (data/get-path-from-uri uri)
            dest-format (if file-ext
                          (keyword file-ext)
                          :transit)]
        {:status  200
         :headers {"Content-Type" (data/content-type-for dest-format)}
         :body    (data/write-data-as (get-thing path)
                                      dest-format)}))
    (GET "/api/*" {:keys [uri]}
      {:status 308
       :headers {"Location" (-> uri (data/get-path-from-uri) (data/get-api-uri-from-path :transit))}})))