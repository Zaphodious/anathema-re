(ns anathema-re.data-layer
  (:require [cljs.core.async :as async]
            [anathema-re.data :as data]
            [cognitect.transit :as transit]))

(defn get-under-path [path]
  (let [ret-chan (async/promise-chan)]
    (-> (.fetch js/window (data/get-uri-from-path path))
        (.then (fn [a] (.text a)))
        (.then (fn [a] (async/put! ret-chan
                                   (transit/read
                                     (transit/reader :json)
                                     a)))))


    ret-chan))

(defn put-under-path! [path])
