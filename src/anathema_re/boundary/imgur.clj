(ns anathema-re.boundary.imgur
  (:require [org.httpkit.client :as client]
            [integrant.core :as ig]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [cheshire.core :as chesh])
  (:import [org.apache.commons.codec.binary Base64]
           [org.apache.commons.io IOUtils]
           (java.io InputStream)))

(def imgur-upload-url
  "https://api.imgur.com/3/image")

(defn input-stream->bytes [input-stream]
  (let [ary (byte-array (.length input-stream))]
        ;is (java.io.FileInputStream. file)]
    (.read input-stream ary)
    ;(.close is)
    ary))

(defn file->base64-string [input-stream]
  (->> input-stream
       ;io/reader
       ;(IOUtils/toByteArray ^InputStream input-stream)
       (.bytes)
       b64/encode
       String.))

(defn make-query-map [api-key input-stream]
  (println "file is " input-stream)
  {:form-params    {:image (file->base64-string input-stream)}
   :headers {"Authorization" (str "Client-ID " api-key)}})

(defn upload-image [api-key input-stream]
  (:data (chesh/decode (:body @(client/post imgur-upload-url (make-query-map api-key input-stream))) true)))

(defmethod ig/init-key :anathema-re.boundary/imgur [_ {:keys [environ]}]
  (partial upload-image (:imgur-id environ)))