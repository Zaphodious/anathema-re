(ns anathema-re.data-layer
  (:require [cljs.core.async :as async]
            [anathema-re.data :as data]
            [cognitect.transit :as transit]
            [com.rpl.specter :as sp]))

(def page-temp-state
  (atom {:character {}
         :rulebook {}
         :player {}
         :current-player nil}))

(defmulti add-entity :category)
(defmethod add-entity :default [{:keys [category key]
                                 :as entity}]
  (async/go
    (sp/transform [sp/ATOM (sp/keypath category key)]
                  (constantly entity)
                  page-temp-state)))

(defmethod add-entity :player-full [{:keys [category key
                                            character rulebook]
                                     :as entity}]
  (async/go
    (let [dumb-player (assoc entity
                        :category :player
                        :character (vec (map first character))
                        :rulebook (vec (map first rulebook)))]
      (-> (into character rulebook)
          (into {key dumb-player})
          (map second)
          (map add-entity)))))

(defn put-under-path! [path thing]
  (async/go
    (sp/transform [sp/ATOM (apply sp/keypath path)]
                  (constantly thing)
                  page-temp-state)))


(defn add-entity-to-page-state! [{:keys [category key]
                                  :as entity}]
  (println "adding " key ", the " category)
  (sp/transform [sp/ATOM (sp/keypath category key)]
                (fn [_] entity)
                page-temp-state))

(defn insert-entity [{:keys [character rulebook full key category]
                      :as   player-entity}]
  (if full
    (let [char-vec (vec (map first character))
          rulebook-vec (vec (map first rulebook))
          basic-player (-> player-entity
                          (assoc :character char-vec)
                          (assoc :rulebook rulebook-vec))]
      (sp/transform [sp/ATOM]
                    (fn [a]
                      (-> a
                          (assoc :player {key basic-player})
                          (assoc :character character)
                          (assoc :rulebook rulebook)))
                    page-temp-state))
      ;(println "atom is now " page-temp-state))

    (add-entity-to-page-state! player-entity)))

(defn sync-path-from-server [path]
  (let [promise-ch (async/promise-chan)]
    (async/go
      (-> (.fetch js/window (data/get-api-uri-from-path path))
          (.then (fn [a] (.text a)))
          (.then (fn [a] (async/put! promise-ch (put-under-path! path (transit/read (transit/reader :json) a)))))))
    promise-ch))

(def starting-page-gets
  [[:rulebook "0"]])

(defn init-app-state [mounting-callback page-path]
  (async/go
    (doall
      (map
        sync-path-from-server
        starting-page-gets)))
  (async/go
    (let [_ (async/<! (sync-path-from-server page-path))]
      (mounting-callback)
      (println "At this point, atom is " page-temp-state))))



(defn get-under-path [path]
  (println "getting " path)
  (let [result (first (sp/select [sp/ATOM (apply sp/keypath path)] page-temp-state))]
    (if result
      result
      (do
        ;(sync-path-from-server path)
        nil))))

(defn ^:export debug-print-state []
  (println "page-temp-state: " page-temp-state))