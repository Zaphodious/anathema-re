(ns anathema-re.boundary.mongo
  (:require [integrant.core :as ig]
            [environ.core :refer [env]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [gzip-util.core :as gz]
            [clojure.java.io :as io]
            [com.rpl.specter :as sp]
            [clojure.data :as cd]
            [cemerick.url :as url :refer [url-decode url-encode]]
            [clojure.core.async :as async]))

(defmulti -write-db-object- (fn [db object]
                              (println "finding thing for " (keyword (:category object)))
                              (keyword (:category object))))

(defn write-db-object [db {object :view :as viewmap}]
  (let [real-db (if (map? db) (:db db) db)]
    (-write-db-object- real-db (if object object viewmap))))

(defmulti -read-db-object- (fn [db category id]
                             category))

(defn read-db-object [db [category id :as path]]
  (let [real-db (if (map? db) (:db db) db)]
    {:path path
     :view (-read-db-object- real-db category id)}))

(defn drop-category [db category]
  (let [real-db (if (map? db) (:db db) db)]
    (mc/drop real-db (name category))))

(def entity-collection "entities")
(def character-collection "character")
(def rulebook-collection "rulebook")
(def player-collection "player")

(defn- connect-to-db! [db-uri]
  (mg/connect-via-uri db-uri))

(defn- put-entity-in-db! [db thingy collection-name]
  (mc/update db collection-name
             {:_id (or (:key thingy) "nill")}
             {:name     (:name thingy)
              :category (:category thingy)
              :type     (:type thingy)
              :subtype  (:subtype thingy)
              :player    (:player thingy)
              :data     (gz/str->gzipped-bytes (url-encode (pr-str thingy)))}
             {:upsert true}))

(defn- extract-data [entry-map]
  (url-decode (gz/gzipped-input-stream->str (io/input-stream (:data entry-map)) "UTF-8")))
(defn- get-entity-from-db [db coll-name key]
  (read-string (extract-data (mc/find-map-by-id db coll-name (str key)))))

(defn- all-entities-to-file [db filename coll-name]
  (spit (str filename ".edn")
        (->> (mc/find-maps db coll-name)
             (map extract-data)
             (map read-string)
             (vec))))

(defn- all-players-to-file [db filename]
  (spit (str filename ".edn")
        (vec (mc/find-maps db "players"))))

(defn- put-thing-in-db! [db {:keys [category key] :as thingy}]
  (println category " " key " " thingy)
  (mc/update db (name (:category thingy))
             {:_id (or key "nill")}
             thingy
             {:upsert true}))

(defn- put-player-in-db! [db player-thing]
  (mc/update db player-collection
             {:_id (or (:key player-thing "nill"))}
             player-thing
             {:upsert true}))

(defn- get-thing-from-db [db category id]
  (when (and db category id)
    (-> (mc/find-map-by-id db (name category) id)
        (dissoc :_id)
        (assoc :category (keyword category)))))

(defn- get-player-from-db [db player-id]
  (assoc (dissoc (mc/find-map-by-id db player-collection player-id) :_id) :category :player))

(defn- get-path-parent-from-db [db path]
  (let [category (first path)
        id (second path)
        get-fn (if (= :player category)
                 get-player-from-db
                 get-entity-from-db)]
    (get-fn db id)))

(defmethod -write-db-object- :default
  [db player-object]
  (put-thing-in-db! db player-object))

(defmethod -read-db-object- :default
  [db category id]
  (get-thing-from-db db category id))

(defmethod -write-db-object- :character
  [db character-object]
  (put-entity-in-db! db character-object character-collection))

(defmethod -read-db-object- :character
  [db category id]
  (get-entity-from-db db character-collection id))

(defmethod -write-db-object- :rulebook
  [db rulebook-object]
  (put-entity-in-db! db rulebook-object rulebook-collection))

(defmethod -read-db-object- :rulebook
  [db category id]
  (get-entity-from-db db rulebook-collection id))

(defn make-backup-file-name [category]
  (str "dev-" category "-backup.edn"))

(defn backup-dev-db! [db]
  (let [collections [player-collection rulebook-collection character-collection]]
    (->> collections
         (map (fn [a] (mc/find-maps db a)))
         (reduce into)
         (map (fn [{:keys [_id category]}] [(keyword category) _id]))
         (map (fn [[category id]] (-read-db-object- db category id)))
         (reduce (fn [r {:keys [category] :as a}]
                   (sp/transform [(sp/keypath category)]
                                 (fn [v] (conj v a))
                                 r))
                 {})
         (map (fn [[k v]]
                (spit (make-backup-file-name (name k))
                      (vec v)))))))

(defn restore-dev-db! [db]
  (let [collections [player-collection rulebook-collection character-collection]]
    (->> collections
         (map make-backup-file-name)
         (map slurp)
         (map read-string)
         (reduce into [])
         (map (fn [a] (-write-db-object- db a))))))


(defn remove-all-in-category [db category]
  (mc/drop-indexes db (name category)))

(defn remove-object [db object]
  (mc/drop-index db (name (:category object)) (:key object)))

(defn get-from-path [{:keys [db conn] :as mong-map}
                     [category id & entity-path :as path]]
  (let [entity (-read-db-object- db category (str id))]
    (if (empty? entity-path)
      entity
      (get-in entity entity-path))))

(defn put-in-path! [{:keys [db conn] :as mong-map}
                    [category id & entity-path :as path]
                    put-value]
  (println "Adding " put-value " as " entity-path "for the " category ", " id)
  (let [entity (get-from-path mong-map [category (str id)])
        new-entity (if entity-path
                     (assoc-in entity entity-path put-value)
                     put-value)
        put-result (-write-db-object- db new-entity)]))

(defmethod ig/init-key ::connection [_ {:keys [environ] :as opts}]
  (let [db-uri (:mongodb-uri environ)
        connec (mg/connect-via-uri db-uri)]
    ;(restore-dev-db! (:db connec))
    connec))

(defmethod ig/init-key :anathema-re.boundary/get [_ {:keys [mongo]}]
  #(#'get-from-path mongo %))
(defmethod ig/init-key :anathema-re.boundary/put [_ {:keys [mongo]}]
  #(#'put-in-path! mongo %1 %2))