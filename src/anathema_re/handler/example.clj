(ns anathema-re.handler.example
  (:require [compojure.core :refer :all]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key :anathema-re.handler/example [_ options]
  (context "/example" []
    (GET "/" []
      (io/resource "anathema_re/handler/example/example.html"))))
