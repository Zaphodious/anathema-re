(ns anathema-re.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [duct.core :as duct]
            [anathema-re.boundary.environ]
            [anathema-re.boundary.mongo]))

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys (or (duct/parse-keys args) [:duct/daemon])]
    (-> (duct/read-config (io/resource "anathema_re/config.edn"))
        (duct/prep keys)
        (duct/exec keys))))
