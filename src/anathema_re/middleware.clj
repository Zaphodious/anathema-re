(ns anathema-re.middleware
  (:require [integrant.core :as ig]
            [ring.middleware.gzip :as rg]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.multipart-params :as mult]))

(defmethod ig/init-key :anathema-re.middleware/gzip [_ _]
  rg/wrap-gzip)

(defmethod ig/init-key :anathema-re.middleware/file [_ _]
  mult/wrap-multipart-params)


;(defmethod ig/init-key :duct.middleware.web/defaults [_ defaults]
;  (fn [a]
;    (-> a
;        rg/wrap-gzip
;        (defaults/wrap-defaults defaults))))

