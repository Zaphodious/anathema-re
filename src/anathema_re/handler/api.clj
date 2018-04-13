(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]))

(defn read-path [path-vec]
  (->> path-vec
       (map read-string)
       (map (fn [a] (if (symbol? a)
                        (keyword (name a))
                        a)))))
(defn split-uri [uri]
  (-> uri
    (str/replace "/api/" "")
    (str/split #"/")))
(defn get-path-from-uri [uri]
  (-> uri
      (split-uri)
      (read-path)
      vec))


(defmethod ig/init-key :anathema-re.handler/api [_ {:keys [get-thing put-thing!]
                                                    :as opts}]
  (GET "/api/*" [format
                 :as
                 {:keys [uri headers query-string]
                  :as request}]
    (let [path (get-path-from-uri uri)]
      {:status  200
       :headers {"Content-Type" "text/json"}
       :body
                (data/write-data-as (get-thing path) (if format (keyword format) :transit))})))