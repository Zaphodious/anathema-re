(ns anathema-re.ui
  (:require [rum.core :as rum]))

(rum/defc app-core [page-url]
  [:#app-frame
   [:h1 "Hello!"]
   [:p "testing"]
   [:p (str "the page url is " page-url)]])


(rum/defc homepage [page-url]
  [:html {:lang "en" :class "home"}
   [:head
    [:link {:rel "stylesheet" :href "/style/main.css"}]]
   [:body
    [:#appmount (app-core page-url)]
    [:script {:src "/js/main.js"}]]])
