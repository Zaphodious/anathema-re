(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]
            [clojure.core.async :as async]))


(defmethod ig/init-key :anathema-re.handler/api [_ {:keys [get-thing put-thing!]
                                                    :as opts}]
  (GET "/api/*" {:keys [uri headers query-string]
                  :as request}
    (let [path (data/get-path-from-uri uri)
          dest-format (if (or (= "" query-string) (nil? query-string))
                        :transit
                        (keyword (second (str/split query-string #"="))))]
      {:status  200
       :headers {"Content-Type" (data/content-type-for dest-format)}
       :body    (data/write-data-as (async/<!!  (get-thing path))
                                    dest-format)})))