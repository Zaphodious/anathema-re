(ns anathema-re.data-layer
  (:require [cljs.core.async :as async]
            [anathema-re.data :as data]
            [cognitect.transit :as transit]
            [com.rpl.specter :as sp]
            [ajax.core :as ajax]))

(def page-temp-state
  (atom {:character {}
         :rulebook {}
         :player {}
         :current-player nil}))

(defn sync-path-from-server-old [path]
  (-> (.fetch js/window (data/get-api-uri-from-path path))
      (.then (fn [a] (.text a)))
      (.then (fn [a] (sp/transform [sp/ATOM (sp/keypath path)]
                                   (fn [b] (transit/read (transit/reader :json) a))
                                   page-temp-state)))))

(defn sync-path-from-server [path]
  (ajax/GET (data/get-api-uri-from-path path)
            {:api (js/XMLHttpRequest.)
             :handler (fn [a] (sp/transform [sp/ATOM (sp/keypath path)]
                                            (fn [b] (transit/read (transit/reader :json) a))
                                            page-temp-state))}))

(def starting-page-gets
  [[:rulebook "0"]])

(defn init-app-state []
  (map
    sync-path-from-server
    starting-page-gets))



(defn get-under-path [path]
  (let [result (first (sp/select [sp/ATOM (sp/keypath path)] page-temp-state))]
    (if result
      result
      (do
        (sync-path-from-server path)
        nil))))

(defn put-under-path! [path])
