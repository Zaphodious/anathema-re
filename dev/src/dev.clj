(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [anathema-re.boundary.environ]
            [anathema-re.boundary.mongo]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [duct.repl.figwheel :refer [cljs-repl]]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "dev.edn")))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! (comp duct/prep read-config))

(defn backup-db []
  (anathema-re.boundary.mongo/backup-dev-db! (:db (:anathema-re.boundary.mongo/connection system))))
(defn restore-db []
  (anathema-re.boundary.mongo/restore-dev-db! (:db (:anathema-re.boundary.mongo/connection system))))