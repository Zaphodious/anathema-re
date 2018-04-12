(ns anathema-re.ui
  (:require [rum.core :as rum]))

(rum/defc app-core [page-url]
  [:#app-frame
   [:.page-title [:h1 "Anathema"]]
   [:#menu [:ul [:li [:i.material-icons.menu-icon "apps"] [:span.label "Home"]]
            [:li [:i.material-icons.menu-icon "settings"] [:span.label "Settings"]]
            [:li [:i.material-icons.menu-icon "storage"] [:span.label "My Profile"]]
            [:li [:i.material-icons.menu-icon "face"] [:span.label "My Characters"]]
            [:li [:i.material-icons.menu-icon "book"] [:span.label "My Rulebooks"]]]]
   [:#content [:.page (str "the thing is " page-url)]]])


(rum/defc homepage [page-url]
  [:html {:lang "en" :class "home"}
   [:head
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:link {:rel "preload" :href "/style/main.css" :as "style"}]
    [:link {:rel "manifest" :href "/manifest.json"}]
    [:link {:rel "stylesheet" :href "/style/main.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "/style/font.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/icon?family=Material+Icons" :type "text/css"}]
    [:link {:rel "icon" :type "image/png" :href "/img/gilted-logo.jpg"}]]


   [:body
    [:#appmount (app-core page-url)]
    [:script {:src "/js/main.js"}]]])
