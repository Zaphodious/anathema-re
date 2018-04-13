(ns anathema-re.middleware
  (:require [integrant.core :as ig]
            [ring.middleware.gzip :as rg]
            [ring.middleware.defaults :as defaults]))

(defmethod ig/init-key :anathema-re.middleware/gzip [_ _]
  (fn [a]
    (println "gzipping " a)
    (rg/wrap-gzip a)))

;(defmethod ig/init-key :duct.middleware.web/defaults [_ defaults]
;  (fn [a]
;    (-> a
;        rg/wrap-gzip
;        (defaults/wrap-defaults defaults))))

