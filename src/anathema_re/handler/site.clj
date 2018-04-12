(ns anathema-re.handler.site
  (:require [compojure.core :refer :all :as com]
            [compojure.route :as comp-route]
            [ring.util.response :as ring-response]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [anathema-re.ui :as ui]
            [anathema-re.style :as style]
            [rum.core :as rum]))

(defn make-html5 [render-str]
  (str "<!DOCTYPE html>\n" render-str))



(defmethod ig/init-key :anathema-re.handler/site [_ options]
  (routes
    (GET "/style/main.css" []
      {:status  200
       :headers {"Content-Type" "text/css"}
       :body
                (#'style/compile-style)})
    (GET "/" []
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body
       (make-html5 (rum/render-static-markup (#'ui/homepage "/")))})))

(defmethod ig/init-key :anathema-re.handler/resources [_ options]
  (comp-route/resources "/"))