(ns anathema-re.style
  (:require [garden.core :as g]))

(def main-style
  [:body {:background-color :blue}])

(defn compile-style []
  (g/css main-style))