(ns anathema-re.data-layer
  (:require [cljs.core.async :as async]
            [anathema-re.data :as data]
            [cognitect.transit :as transit]
            [com.rpl.specter :as sp]
            [alandipert.storage-atom :as statom]))

(def page-temp-state
  (atom {:character {}
         :rulebook {}
         :player {}
         :current-player nil}))

(def auth-cache
  (statom/session-storage (atom {})
                          :g-auth))

(def changes-since-last-push (atom []))

(defn make-mod-path [path]
   (if (= "me" (second path))
     (-> path vec (assoc 1 (or (:current-player @page-temp-state) "010101")))
     path))

(defn make-request-headers []
  (let [token (:token @auth-cache)]
    (clj->js {:headers
              (if token
                {:token token}
                {})})))

(def goog-key-atom (atom ""))

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

(defn put-under-path-and-mark-changed! [path thing]
  (async/take! (put-under-path! path thing)
               (fn [_] (swap! changes-since-last-push (fn [a] (conj a path))))))

(defn add-entity-to-page-state! [{:keys [category key]
                                  :as entity}]
  ;(println "adding " key ", the " category)
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

(defn print-pass [t]
  (println t)
  t)

(defn load-cache-with! [entity-type thing sync-fn]
  (let [type-structure (entity-type thing)]
    (when (vector? type-structure)
      (async/go
        (->> (entity-type thing)
             (map (fn [a] [entity-type a]))
             (map sync-fn)
             doall))))
  thing)

(defn sync-path-from-server [path]
  (let [promise-ch (async/promise-chan)
        mod-path (make-mod-path path)]
    ;(println "Getting the thing for path " mod-path)
    (async/go
      (-> (.fetch js/window (data/get-api-uri-from-path path) (make-request-headers))
          (.then (fn [a] (.text a)))
          (.then (fn [a] (transit/read (transit/reader :json) a)))
          (.then (fn [a] (println "null thing is " a) a))
          (.then (fn [a] (if a a "")))
          (.then (fn [a] (load-cache-with! :character a sync-path-from-server)))
          (.then (fn [a] (load-cache-with! :rulebook a sync-path-from-server)))
          (.then (fn [a] (async/put! promise-ch (put-under-path! mod-path a))))))
    promise-ch))

(defn auth-cache-to-temp-state! []
  (let [auth @auth-cache]
    (when (not (empty? auth))
      (do
        ;(println "syncing this all up and down!")
        (sp/transform [sp/ATOM :current-player] (constantly (:key auth))
                      page-temp-state)
        (sync-path-from-server [:player "me"])))))

(def starting-page-gets
  []);[:rulebook "0"]])


(defn sync-site-key []
  (async/go
    (-> (.fetch js/window "/site-key.txt")
        (.then #(.text %))
        (.then #(print-pass %))
        (.then #(reset! goog-key-atom %)))))

(defn init-app-state [mounting-callback page-path]
  (async/go
    (doall
      (map
        sync-path-from-server
        starting-page-gets)))
  (async/go (auth-cache-to-temp-state!))
  (async/go
    (let [_ (async/<! (sync-path-from-server page-path))]
      (mounting-callback))))
      ;(println "At this point, atom is " page-temp-state))))



(defn get-under-path
  "Takes a vector path, returns either the entity requested or nil. Attempts to get the entity from
  the server if not present. Bear in mind that the UI refreshes automatically when the state atom is changed."
  [path]
  ;(println "getting " path)
  (let [mod-path (make-mod-path path)
        result (first (sp/select [sp/ATOM (apply sp/keypath mod-path)] page-temp-state))]
    (if result
      result
      (do (sync-path-from-server mod-path) nil))))

(defn handle-credential [{:keys [idToken] :as credential}]
  (println "handling credential for " (pr-str credential))
  (println "credential swapped result: "(swap! page-temp-state (fn [a] (assoc a :current-player idToken)))))
;(sp/transform [(sp/keypath :current-player)] (constantly idToken) page-temp-state))

(defn ^:export debug-print-state []
  (println "page-temp-state: " page-temp-state)
  (println "key is " goog-key-atom))