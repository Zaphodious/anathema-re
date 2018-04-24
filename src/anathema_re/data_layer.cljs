(ns anathema-re.data-layer
  (:require [cljs.core.async :as async]
            [anathema-re.data :as data]
            [cognitect.transit :as transit]
            [com.rpl.specter :as sp]
            [alandipert.storage-atom :as statom]
            [clojure.set :as set]
            [clojure.tools.reader :as reader]))

(comment
  "This namespace contains all the code concerned with IO with the anathema server.

  When the page starts, or when a refresh is requested, sync-path-from-server! is called.
  When the server must be updated, update-server! is called. When the user changes
  the state of the program through the UI, the path for the change is stored in
  changes-since-last-push and changes-made-recently is set to :changed. The updater
  checks changes-made-recently, if it sees that changes-made-recently is :changed it is
  changed to :staging, and if its :staging it kicks off the sync handler, unless changes-since-last-push
  is empty in which case it sets changes-made-recently to :synced. ")

(def seconds-between-sync 10)

(def page-temp-state
  (statom/local-storage
    (atom {:character {}
           :rulebook {}
           :player {}
           :player-me nil
           :change-thing (rand)})
    :app-state))

(def auth-cache
  (statom/local-storage (atom {})
                        :g-auth))

(def path-hashes
  (statom/local-storage (atom {})
                        :path-hashes))

(def changes-since-last-push
  (statom/local-storage (atom #{})
                        :last-changes))

(def changes-made-recently
  (statom/local-storage (atom :synced)
                        :changed-recently?)) ;:staging :changed
;(add-watch changes-since-last-push
;           :change-marker
;           (fn [the-key the-atom old-state new-state]
;             (let [change-time (.getTime (js/Date.))]
;               ;(.warn js/console "Made change to state at " change-time)
;               (reset! changes-made-recently :changed))))

(defn can-server-sync? []
  ;(println "Currently, " changes-made-recently)
  (case @changes-made-recently
    :synced false
    :staging (do
               (if (empty? @changes-since-last-push)
                 (do
                   (reset! changes-made-recently :synced)
                   false)
                 true))
    :changed (do
               (reset! changes-made-recently :staging)
               false)))

(defn make-mod-path [path]
   (if (= "me" (second path))
     (-> path vec (assoc 1 (or (:player-me @page-temp-state) "010101")))
     path))

(defn register-path-as-changed! [path]
  (let [mod-path (make-mod-path path)]
    (reset! changes-made-recently :changed)
    (swap! changes-since-last-push (fn [a] (conj a mod-path)))))

(defn make-request-headers
  ([] (make-request-headers {}))
  ([m] (let [token (:token @auth-cache)]
         (clj->js
           (merge m
             {:headers
              (if token
                {:token token}
                {})})))))

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
  (println "Putting " thing " under " path)
  (async/go
    (sp/transform [sp/ATOM (apply sp/keypath (make-mod-path path))]
                  (constantly thing)
                  page-temp-state)))

(defn put-under-path-and-mark-changed! [path thing]
  (async/take! (put-under-path! path thing)
               (fn [_] (register-path-as-changed! path))))

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
        mod-path (make-mod-path path)
        current-thing-hash (sp/select-first [sp/ATOM (sp/keypath mod-path)] path-hashes)] ;Without applying, keypath of a path is that path as the key
    ;(println "Getting the thing for path " mod-path)
    (async/go
      (-> (.fetch js/window (str (data/get-api-uri-from-path path) "?got-hash=" current-thing-hash)
                  (make-request-headers))
          (.then (fn [a] (if (= (.-status a)
                                304)
                           (throw (js/Error. (str "Resource under "mod-path" not changed")))
                           a)))
          (.then (fn [a] (if (.-ok a)
                           a
                           (throw (js/Error. (str "Something went wrong. HTTP status was " (.status a)))))))
          (.then (fn [a] (do (sp/transform [sp/ATOM (sp/keypath mod-path)]
                               (constantly (js->clj (.get (.-headers a) "hash")))
                               path-hashes)
                           a)))
          (.then (fn [a] (.text a)))
          (.then (fn [a] (transit/read (transit/reader :json) a)))
          (.then (fn [a] (println "null thing is " a) a))
          (.then (fn [a] (if a a "")))
          (.then (fn [a] (load-cache-with! :character a sync-path-from-server)))
          (.then (fn [a] (load-cache-with! :rulebook a sync-path-from-server)))
          (.then (fn [a] (async/put! promise-ch (put-under-path! mod-path a))))
          (.catch (fn [a] (do (println "Error on sync - " a)
                              false)))
          (.then (fn [a] (async/put! promise-ch a)))))
    promise-ch))

(defn auth-cache-to-temp-state! []
  (let [auth @auth-cache]
    (when (not (empty? auth))
      (do
        ;(println "syncing this all up and down!")
        (sp/transform [sp/ATOM :player-me] (constantly (:key auth))
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


(defn update-server! [success-fn fail-fn path]
  (when path
    (async/go
      (let [mod-path (make-mod-path path)
            thing-to-sync (get-under-path mod-path)
            this-body (data/write-data-as thing-to-sync :transit)]
        (println "Syncing " thing-to-sync " under " mod-path " as "this-body)
        (println "Trying to sync this up- " mod-path)
        (-> (js/fetch (str (data/get-api-uri-from-path mod-path :transit) "?got-hash=" (get @path-hashes mod-path))
                      (make-request-headers {:method "PUT"
                                             :body   this-body}))
            (.then (fn [a] (if (= (.-status a)
                                  304)
                             (throw (js/Error. "Resource under "mod-path" not changed"))
                             a)))
            (.then (fn [a] (if (.-ok a)
                             a
                             (throw (js/Error. (str "Something went wrong. HTTP status was " (.status a)))))))
            (.then (fn [a] (.text a)))
            (.then (fn [a] (transit/read (transit/reader :json) a)))
            (.then (fn [a] (println "null thing is " a) a))
            (.then (fn [a] (if a a "")))
            (.then (fn [a] (map (fn [[k v]] (sp/transform [sp/ATOM (sp/keypath k)] (constantly v) path-hashes)))))
            (.then (fn [a] (success-fn mod-path)))
            (.catch (fn [a]
                      (println "error is " a)
                      (fail-fn mod-path))))))))

(defn update-dirty-paths! []
  (async/go
    (let [success-fn (fn [a] (println "Successfully synced " a)
                             (swap! changes-since-last-push
                                    (fn [b]
                                      (println "removing "a" from the sync set")
                                      (set (remove #(= a %) b)))))
          fail-fn (fn [path] (println "didn't sync "path", keeping it on the sync list."))
          update-path (partial update-server! success-fn fail-fn)]
      (println "Success fn is " success-fn)
      (println "Fail fn is " fail-fn)
      (println "update-path is " update-path)
      (println "changes are " changes-since-last-push)
      (doall
        (map update-path (vec @changes-since-last-push))))))

(defn handle-credential [{:keys [idToken] :as credential}]
  (println "handling credential for " (pr-str credential))
  (println "credential swapped result: "(swap! page-temp-state (fn [a] (assoc a :player-me idToken)))))
;(sp/transform [(sp/keypath :player-me)] (constantly idToken) page-temp-state))

(defn init-app-state [mounting-callback page-path]
  (async/go
    (doall
      (map
        sync-path-from-server
        starting-page-gets)))
  (async/go (auth-cache-to-temp-state!))
  (async/go (js/setInterval (fn [] (when (can-server-sync?)
                                     (update-dirty-paths!)))
                            (* 1000 seconds-between-sync)))
  (async/go
    (let [_ (async/<! (sync-path-from-server page-path))]
      (mounting-callback))))

(defn ^:export debug-print-state []
  (println "page-temp-state: " page-temp-state)
  (println "changed paths are " changes-since-last-push)
  (println "key is " goog-key-atom))

(defn ^:export read-statement [s]
  (reader/read-string s))

(defn put-image-under-path! [path image-blob]
  (let [mod-path (make-mod-path path)
        putting-map (make-request-headers
                      {:body image-blob
                       :method :POST})]
    (println "image is " image-blob)
    (-> (js/fetch "/api/img" putting-map)
        (.then (fn [a] (if (= (.-status a) 304)
                         (throw (js/Error. "Resource under "mod-path" not changed"))
                         a)))
        (.then (fn [a] (if (.-ok a) a
                         (throw (js/Error. (str "Something went wrong. HTTP status was " (.status a)))))))
        (.then #(.text %))
        (.then #(put-under-path-and-mark-changed! mod-path %))
        (.then (fn [a] (update-server! println println mod-path)))
        (.catch (fn [a] (println "Image didn't take."))))))
