(ns anathema-re.data
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]
            [clojure.spec.gen.alpha :as sg]
            [cognitect.transit :as transit]
            [clojure.string :as str]
            [clojure.tools.reader :as reader]
    #?(:clj [hashids.core :as h])
    #?(:clj [clojure.java.io :as io]))
  #?(:clj
     (:import [java.io ByteArrayOutputStream]
              [org.apache.commons.io IOUtils]
              [org.httpkit BytesInputStream])))

(s/def ::id (s/and string? #(not (empty? %))))

(s/def ::category (s/or :keyword? keyword? :string? ::id))

(s/def ::navigator (s/or :keyword keyword? :string-key ::id :index (s/and int? #(<= 0 %))))

(s/def ::path (s/+ ::navigator))

(s/def ::swapper (s/fspec :args any?
                          :ret any?))

(s/def ::game-entity (s/keys :req-un [::category ::id]))

(s/def ::view any?)

(s/def ::viewmap (s/keys :req-un [::path ::view]))

(s/fdef change-game-entity
        :args (s/cat :path-in ::path, :game-entity ::game-entity, :new-thing any?)
        :ret ::game-entity)

(defn change-game-entity [path-in game-entity
                          new-thing]
  (sp/transform [(sp/keypath path-in)]
                (fn [a] new-thing)
                game-entity))

(defn to-transit [thing json? verbose?]
  #?(:clj
     (let [out (ByteArrayOutputStream. 4096)
           writer (transit/writer out (if json?
                                        (if verbose? :json-verbose :json)
                                        :msgpack))]
       (transit/write writer thing)
       (.toString out))
     :cljs
     (transit/write (transit/writer (if json?
                                      (if verbose? :json-verbose :json)
                                      :msgpack))
                    thing)))
#?(:clj
    (defn from-transit [thing json? verbose?]
       (let [in (if (string? thing)
                   (.getBytes thing)
                   thing)
              reader (transit/reader in (if json?
                                          (if verbose? :json-verbose :json)
                                          :msgpack))]
          (transit/read reader))))

(defn write-data-as [thing format]
  (case format
    :edn (pr-str thing)
    :json (to-transit thing true true)
    :transit (to-transit thing true false)
    :msgpack (to-transit thing false false)))

#?(:clj
    (defn read-data-as [thing format imgur]
      (case format
        :edn (read-string (if (string? thing) thing (IOUtils/toString thing)))
        :json (from-transit thing true true)
        :transit (from-transit thing true false)
        :msgpack (from-transit thing false false)
        :img (:link (imgur thing)))))

(defn content-type-for [format]
  (case (keyword (name format))
    :edn "text/edn"
    (:json :transit) "text/json"
    :msgpack "application/msgpack"
     "text/json"))

#?(:clj (def ^:private hash-ops {:salt "Exalted Is Best Game!"}))
(defn new-id []
  #?(:clj
           (h/encode hash-ops (rand-int 99999) (rand-int 99999) (rand-int 99999))
     :cljs (str "temp_" (.toLocaleDateString (js/Date.)) (rand-int 999999) (rand-int 999999))))

(defn print-pass [a]
  (println "vecci is " (pr-str a))
  a)

(defn read-path [path-vec]
  (->> path-vec
       (filter #(not (empty? %)))
       (sp/transform [(sp/nthpath 1)] pr-str) ; Prevents ids from being read as numbers.
       (map reader/read-string)
       (map (fn [a] (if (symbol? a)
                      (keyword (name a))
                      a)))))
(defn split-uri [uri]
  (-> uri
      (str/replace "/api/" "")
      (str/split #"\.")
      first
      (str/split #"/")
      print-pass))
(defn get-path-from-uri [uri]
  (if uri
    (-> uri
        (split-uri)
        (read-path)
        vec)
    []))

(defn get-api-uri-from-path
  ([path] (get-api-uri-from-path path :transit))
  ([path format] (str
                   (->> path
                     (map (fn [a] (if (number? a)
                                    (str a)
                                    (name a))))
                     (map (fn [a] (str "/" a)))
                     (reduce str)
                     (str "/api"))
                   (str "." (name format)))))

(defn get-navigation-uri-from-path
  [path]
  (str
    (->> path
         (map (fn [a] (if (number? a)
                        (str a)
                        (name a))))
         (map (fn [a] (str "/" a)))
         (reduce str))))

(defn is-owner? [player-id path get-thing]
  (let [get-the-key (fn [a] (-> (take 2 path) (vec) (conj a) (get-thing) (= player-id)))]
    (or (get-the-key :key) (get-the-key :owner))))

(def imgur-thumb-types
  {:small-square "s"
   :big-square "b"
   :small-thumbnail "t"
   :medium-thumbnail "m"
   :large-thumbnail "l"
   :huge-thumbnail "h"})

(defn modify-imgur-url [imgur-url thumb-type]
  (if (and imgur-url thumb-type)
    (do
      (println "url is " imgur-url)
      (if (re-matches #".*i.imgur.*" imgur-url)
        (let [[pre :as imgur-split] (str/split imgur-url #".png|.jpg|.gif|.gifv")
              thumb-suffix (get imgur-thumb-types thumb-type)
              mod-url (str pre thumb-suffix ".png")]
          (println "new image is " mod-url)
          mod-url)))

    imgur-url))
