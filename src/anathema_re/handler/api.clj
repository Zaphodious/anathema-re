(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]
            [clojure.core.async :as async]))

(defn fill-vec-of [vec-of category-key get-thing]
  (->> vec-of
       (map (fn [a] (conj [category-key] a)))
       (map get-thing)
       (map (fn [a] [(:key a) a]))
       (into {})))

(defn get-full-player-data [path get-thing full?]
  (if (and full? (read-string full?))
    (let [{:keys [character rulebook]
           :as provincial-player}
          (get-thing path)
          filled-rulebook (fill-vec-of rulebook :rulebook get-thing)
          filled-character (fill-vec-of character :character get-thing)]
      (-> provincial-player
          (assoc :rulebook filled-rulebook)
          (assoc :character filled-character)
          (assoc :category :player-full)))
    (get-thing path)))

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
         :headers {"Content-Type" (data/content-type-for dest-format)
                   "Cache-Control" "no-cache, no-store, must-revalidate"}
         :body    (data/write-data-as (if (= :player (first path))
                                        (get-full-player-data path get-thing full)
                                        (get-thing path))
                                      dest-format)}))
    (GET "/api/*" {:keys [uri]}
      {:status 308
       :headers {"Location" (-> uri (data/get-path-from-uri) (data/get-api-uri-from-path :transit))}})))