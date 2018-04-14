(ns anathema-re.ui
  (:require [rum.core :as rum]
            [clojure.core.async :as async]
            [anathema-re.data :as data]))

(defmulti page-for #(-> % :path first))


(defn set-data-atom! [path get-thing state-atom]
  #?(:cljs (async/take! (get-thing path) (fn [a]
                                           (let [current @state-atom]
                                             (when (not (= a current))
                                               (reset! state-atom a)))))
     :clj (reset! state-atom (async/<!! (get-thing path)))))

#?(:clj
    (rum/defc loading-page [{:keys [path get-thing] :as optsmap}]
      (let [{:keys [name img] :as thing} (get-thing path)]
        [:.interior
         [:img {:src (if img img "https://i.imgur.com/Ij692NO.jpg")}]
         [:p (str "Loading page for " name)]])))

(rum/defc app-core < rum/reactive
  [{:keys [path get-thing reactive-atom] :as optsmap}]
  (when reactive-atom (rum/react reactive-atom))
  (let [{:keys [name category]
         :as entity-to-render} (get-thing path)
        page #?(:clj (loading-page optsmap)
                :cljs (page-for optsmap))]
    (println "thing is " entity-to-render)
    [:#app-frame
     [:.page-title [:h1 name]]
     [:#menu [:ul [:li [:i.material-icons.menu-icon "apps"] [:span.label "Home"]]
              [:li [:i.material-icons.menu-icon "settings"] [:span.label "Settings"]]
              [:li [:i.material-icons.menu-icon "storage"] [:span.label "My Profile"]]
              [:li [:i.material-icons.menu-icon "face"] [:span.label "My Characters"]]
              [:li [:i.material-icons.menu-icon "book"] [:span.label "My Rulebooks"]]]]
     [:#content [:.page page]]]))

(rum/defc homepage [{:keys [path get-thing] :as optsmap}]
  [:html {:lang "en" :class "home"}
   [:head
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:link {:rel "preload" :href "/style/main.css" :as "style"}]
    [:link {:rel "preload" :href "/js/main.js" :as "script"}]
    [:link {:rel "preload" :href (data/get-api-uri-from-path path)
            :as  "fetch" :type "text/json"}]
    [:link {:rel "manifest" :href "/manifest.json"}]
    [:link {:rel "stylesheet" :href "/style/main.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "/style/font.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/icon?family=Material+Icons" :type "text/css"}]
    [:link {:rel "icon" :type "image/png" :href "/img/gilted-logo.jpg"}]]


   [:body
    [:#appmount (app-core optsmap)]
    [:script {:src "/js/main.js"}]]])


(rum/defc root-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:img {:src "/img/gilted-logo.jpg"}]
   [:.navbutton "Characters"]])

(rum/defc character-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:p (str "thing is at " path " that can be gotten with " get-thing)]])

(rum/defc rulebook-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:p (str "thing is at " path " that can be gotten with " get-thing)]])

(rum/defc player-page
  [{:keys [path get-thing put-thing!]}]
  [:.interior
   (str (get-thing path))])

(defmethod page-for nil
  [optmap] (root-page optmap))
(defmethod page-for :home
  [optmap] (root-page optmap))
(defmethod page-for :character
  [optmap] (character-page optmap))
(defmethod page-for :player
  [optmap] (player-page optmap))
(defmethod page-for :rulebook
  [optmap] (rulebook-page optmap))