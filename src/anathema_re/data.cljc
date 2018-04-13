(ns anathema-re.data
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]
            [clojure.spec.gen.alpha :as sg]
            [cognitect.transit :as transit]
    #?(:clj [hashids.core :as h]))
  #?(:clj (:import (java.io ByteArrayOutputStream))))

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

(defn to-transit [thing verbose?]
  #?(:clj
     (let [out (ByteArrayOutputStream. 4096)
           writer (transit/writer out (if verbose? :json-verbose :json))]
       (transit/write writer thing)
       (.toString out))
     :cljs
     (transit/write (transit/writer (if verbose? :json-verbose :json))
                    thing)))

(defn write-data-as [thing format]
  (case format
    :edn (pr-str thing)
    :json (to-transit thing true)
    :transit (to-transit thing false)))

(defn content-type-for [format]
  (case (keyword format)
    :edn "text/edn"
    :json "text/json"
    :transit "text/json"))

#?(:clj (def ^:private hash-ops {:salt "Exalted Is Best Game!"}))
(defn new-id []
  #?(:clj
           (h/encode hash-ops (rand-int 99999) (rand-int 99999) (rand-int 99999))
     :cljs (str "temp_" (.toLocaleDateString (js/Date.)) (rand-int 999999) (rand-int 999999))))
