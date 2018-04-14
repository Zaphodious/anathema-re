(ns anathema-re.handler.site
  (:require [compojure.core :refer :all :as com]
            [compojure.route :as comp-route]
            [ring.util.response :as ring-response]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [anathema-re.ui :as ui]
            [anathema-re.style :as style]
            [rum.core :as rum]
            [ring.middleware.gzip :as gz]
            [anathema-re.data :as data]
            [clojure.string :as str]))

(defn make-html5 [render-str]
  (str "<!DOCTYPE html>\n" render-str))



(defmethod ig/init-key :anathema-re.handler/site [_ {:keys [js get-thing put-thing!] :as options}]
  (routes
    (GET "/style/main.css" []
      {:status  200
       :headers {"Content-Type" "text/css"}
       :body
                (#'style/compile-style)})
    (GET "/" []
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body
                (make-html5 (rum/render-static-markup (#'ui/homepage {:path [:home]
                                                                      :get-thing get-thing
                                                                      :put-thing! put-thing!})))})
    (GET "/character/*" {:keys [uri headers query-string]
                         :as request}
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (make-html5 (rum/render-static-markup (#'ui/homepage {:path (data/get-path-from-uri uri)
                                                                      :get-thing get-thing
                                                                      :put-thing! put-thing!})))})
    (GET "/player/*" {:keys [uri headers query-string]
                         :as request}
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (make-html5 (rum/render-static-markup (#'ui/homepage {:path (data/get-path-from-uri uri)
                                                                      :get-thing get-thing
                                                                      :put-thing! put-thing!})))})))

(defmethod ig/init-key :anathema-re.handler/resources [_ options]
  (comp-route/resources "/"))

(defmethod ig/init-key :anathema-re.handler/js [_ {:keys [js] :as options}]
  (comp-route/resources "/js/"))

(defmethod ig/init-key :duct.handler/static [_ _]
  (constantly nil))
(defmethod ig/init-key :duct.handler.static.ok [_ _]
  (constantly nil))