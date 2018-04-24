(ns anathema-re.handler.api
  (:require [integrant.core :as ig]
            [compojure.core :refer :all :as com]
            [clojure.string :as str]
            [anathema-re.data :as data]
            [clojure.core.async :as async]
            [ring.middleware.gzip :as rg]
            [clojure.set :as set]
            [clojure.java.io :as io])
  (:import (java.io InputStream)
           (org.apache.commons.io IOUtils)))

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
                     (-> player-me
                         (set/rename-keys {:user-id :key :picture-url :img}))
                     (select-keys existant-player
                                  [:name :img]))]


    (println "player is " (pr-str new-player))
    (async/go (put-thing! player-path new-player))
    new-player))

(defn id-for [{:keys [goog-oauth] :as opts} {:strs [token] :as headers}]
  (println "google auth is " (goog-oauth token))
  (:user-id (goog-oauth token)))

(defn proper-hash [thing-string]
  (str (.hashCode
         (if (string? thing-string)
           thing-string
           (pr-str thing-string)))))

(defn recursive-hash [orig-path get-thing hash-fn]
  (into {}
    (pmap (fn [a] {a (hash-fn (get-thing a))})
          (reduce (fn [a b] (conj a (vec (conj (last a) b)))) [] orig-path))))
  ;(loop [path orig-path
  ;       hashes {}]
  ;  (if (empty? path)
  ;    hashes
  ;    (recur (vec (drop-last path))
  ;           (assoc hashes path (future (hash-fn (get-thing path))))))))


(defmethod ig/init-key :anathema-re.handler/api [_ {:keys [get-thing put-thing! goog-oauth imgur]
                                                    :as opts}]

  ;(doall
  ;  (map (fn [a] (put-thing! [:character a :player] "106295716847506101421"))
  ;       ["963963963" "7777777777" "789789789" "424242"]))
  ;(doall
  ;  (map (fn [a] (put-thing! [:rulebook a :player] "106295716847506101421"))
  ;       ["452452452" "242424" "852852852" "01010101" "0" "1"]))

  (routes
    (POST "/api/img" {:keys [uri headers query-string body]
                       {:strs [file]} :params
                       :as request}
      (let [authed? (:user-id (id-for opts headers))]
        {:status 200
         :body   (:link (imgur body))}))
    (PUT "/api/player/me.:file-ext" []
      {:status 418
       :body "Please use player id directly."})
    (PUT "/api/player/me/*.:file-ext" []
      {:status 418
       :body "Please use player id directly."})
    (PUT "/api/*.:file-ext"  [file-ext full :as
                              {:keys [uri headers query-string body]
                               {:strs [file]} :params
                               :as request}]
      (let [path (data/get-path-from-uri uri)
            owner? (data/is-owner? (id-for opts headers) path get-thing)]
        (println "the logged in id is " (id-for opts headers))
        (println "Path for this is " path)
        (if (and owner? (not (= :owner (last path))))
          (let [dest-format (if file-ext
                              (keyword file-ext)
                              :transit)
                read-in-content (data/read-data-as body dest-format imgur)
                write-result (put-thing! path read-in-content)
                hashes (recursive-hash path get-thing #(.hashCode (data/write-data-as % dest-format)))
                written-hashes (data/write-data-as hashes dest-format)]
            (println "This is what came in - "body)
            (println "Should have written in " read-in-content ", under " path)
            {:status 200
             :body written-hashes})
          {:status 403
           :body "NOT AUTHORIZED!"})))
    (GET "/api/player/me.:file-ext" [file-ext got-hash :as {:keys [headers]}]
      (let [dest-format (if file-ext
                          (keyword file-ext)
                          :transit)
            me-string (data/write-data-as (handle-player-me-data opts headers)
                                          dest-format)
            me-hash (proper-hash me-string)
            same-thing? (= got-hash me-hash)]
        ;(println "header is " headers)
        (if same-thing?
          {:status 304
           :body ""}
          {:status  200
           :headers {"Content-Type"  (data/content-type-for dest-format)
                     "Cache-Control" "no-cache, no-store, must-revalidate"
                     "hash" me-hash}
           :body me-string})))
    (GET "/api/player/me/*.:file-ext" []
      {:status 418
       :body "Please use player id directly."})
    (GET "/api/*.:file-ext"
         [file-ext full got-hash :as
          {:keys [uri headers query-string]
           :as request}]
      (println "file ext is " file-ext)
      (println "full is " full)
      (println "requested hash is " got-hash)
      (println "query is " (pr-str query-string))
      (let [path (data/get-path-from-uri uri)
            dest-format (if file-ext
                          (keyword file-ext)
                          :transit)
            dest-thing (data/write-data-as (if (= :player (first path))
                                             (get-full-player-data path get-thing full)
                                             (get-thing path))
                                           dest-format)
            dest-hash (proper-hash dest-thing)
            same-thing? (= got-hash dest-hash)]
        (println "Same thing? " same-thing?"! dest-hash is " (pr-str dest-hash)
                 " and got-hash is " (pr-str got-hash))
        (if same-thing?
          {:status 304
           :body ""}
          {:status  200
           :headers {"Content-Type" (data/content-type-for dest-format)
                     "Cache-Control" "no-cache, no-store, must-revalidate"
                     "hash" dest-hash}
           :body dest-thing})))
    (GET "/api/*" {:keys [uri]}
      {:status 308
       :headers {"Location" (-> uri (data/get-path-from-uri) (data/get-api-uri-from-path :transit))}})))