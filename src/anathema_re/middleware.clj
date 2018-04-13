(ns anathema-re.middleware
  (:require [integrant.core :as ig]
            [ring.middleware.gzip :as rg]))

(defmethod ig/init-key :anathema-re.middleware/gzip [_ _]
  (fn [a]
    (println "gzipping " a)
    (rg/wrap-gzip a)))