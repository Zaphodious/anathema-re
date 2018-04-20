(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]
            [clojure.core.async :as async]
            [ring.middleware.gzip :as rg]
            [clojure.set :as set]
            [clojure.java.io :as io])
  (:import (java.io InputStream)))

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

(defn handle-player-me-data [{:keys [get-thing put-thing! goog-oauth]} {:strs [token] :as headers}]
  (let [player-me (goog-oauth token)
        player-path [:player (:user-id player-me)]
        existant-player (get-thing player-path)
        new-player (merge
                     {:character [] :rulebook []}
                     existant-player
                     (set/rename-keys player-me {:user-id :key :picture-url :img}))]

    (println "player is " (pr-str new-player))
    (async/go (put-thing! player-path new-player))
    new-player))

(defn id-for [{:keys [goog-oauth] :as opts} {:strs [token] :as headers}]
  (:user-id (goog-oauth token)))


(defmethod ig/init-key :anathema-re.handler/api [_ {:keys [get-thing put-thing! goog-oauth imgur]
                                                    :as opts}]

  ;(doall
  ;  (map (fn [a] (put-thing! [:character a :player] "106295716847506101421"))
  ;       ["963963963" "7777777777" "789789789" "424242"]))
  ;(doall
  ;  (map (fn [a] (put-thing! [:rulebook a :player] "106295716847506101421"))
  ;       ["452452452" "242424" "852852852" "01010101" "0" "1"]))

  (routes
    (GET "/api/player/me.:file-ext" [file-ext :as {:keys [headers]}]
      (let [dest-format (if file-ext
                          (keyword file-ext)
                          :transit)]
        ;(println "header is " headers)
        {:status  200
         :headers {"Content-Type"  (data/content-type-for dest-format)
                   "Cache-Control" "no-cache, no-store, must-revalidate"}
         :body    (data/write-data-as (handle-player-me-data opts headers)
                                      dest-format)}))
    (PUT "/api/*.:file-ext"  [file-ext full :as
                               {:keys [uri headers query-string body]
                                {:strs [file]} :params
                                :as request}]
      (let [path (data/get-path-from-uri uri)
            dest-format (if file-ext
                          (keyword file-ext)
                          :transit)
            read-in-content (data/read-data-as body dest-format imgur)]

        {:status 200
         :body   read-in-content}))

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