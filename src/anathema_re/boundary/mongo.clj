(ns anathema-re.boundary.mongo
  (:require [integrant.core :as ig]
            [somnium.congomongo :as mong]))



(defmethod ig/init-key :anathema-re.boundary/mongo [_ {:keys [environ] :as opts}]
  (let [db-uri (:mongodb-uri environ)
        connec (mong/make-connection db-uri)]
    connec))

(defmethod ig/init-key :anathema-re.boundary/get [_ {:keys [mongo]}])
(defmethod ig/init-key :anathema-re.boundary/put [_ {:keys [mongo]}])