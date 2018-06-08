(ns anathema-re.ui
  (:require [rum.core :as rum]
            [clojure.core.async :as async]
            [anathema-re.data :as data]
            [anathema-re.ui.defs :as auid]))

(defmulti page-for #(-> % :path first))


#?(:clj
    (rum/defc loading-page [{:keys [path get-thing] :as optsmap}]
      (let [{:keys [name img] :as thing} (get-thing path)]
        [:.interior
         [:img {:src (if img img "https://i.imgur.com/Ij692NO.jpg")}]
         [:p (str "Loading page for " name)]])))

(rum/defc app-core < rum/reactive
  [{:keys [path get-thing reactive-atom api-key user-info-get put-thing! auth-response-handler current-user-atom] :as optsmap}]
  (when reactive-atom
    (do (println "reacting to this atom")
        (rum/react reactive-atom)))
  ;(println "thing is definitely " (get-thing path) " at " path)
 ; (println "atomo is " reactive-atom)
  (let [home? (empty? path)
        {:keys [name category]
         :as entity-to-render} (get-thing path)
        current-user (rum/react current-user-atom)
        page (page-for optsmap)]
    ;(println "thing is " entity-to-render)
    [:#app-frame
     [:.page-title [:h1 name]]

     [:#menu [:ul [:li [:i.material-icons.menu-icon "apps"] [:span.label "Home"]]
              [:li [:i.material-icons.menu-icon "settings"] [:span.label "Settings"]]
              [:li [:i.material-icons.menu-icon "face"] [:span.label "My Characters"]]
              [:li [:i.material-icons.menu-icon "book"] [:span.label "My Rulebooks"]]]]
     [:#content
      [:.page page]]]))

(rum/defc homepage [{:keys [path get-thing api-key] :as optsmap}]
  [:html {:lang "en" :class "home"}
   [:head
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:link {:rel "preload" :href "/style/main.css" :as "style"}]
    [:link {:rel "preload" :href "/js/main.js" :as "script"}]
    [:link {:rel "preload" :href (str (data/get-api-uri-from-path path) "?full=true")
            :as  "fetch" :type "text/json"}]
    [:link {:rel "manifest" :href "/manifest.json"}]
    [:link {:rel "stylesheet" :href "/style/main.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "/style/font.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/icon?family=Material+Icons" :type "text/css"}]
    [:link {:rel "icon" :type "image/png" :href "/img/gilted-logo.jpg"}]]
   [:body
    [:#appmount (app-core optsmap)]
    [:script {:src "/sitekey.js"}]
    [:script {:src "/js/main.js"}]]])

(rum/defc form-of [fieldmap]
  [:.form-of [:span.label "Thing"] [:span.field "Some"]])

(rum/defc root-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:img {:src "/img/gilted-logo.jpg"}]
   [:.navbutton "Characters"]])

(rum/defc shell-page [{:keys [path get-thing put-thing!]}]
  [:.interior "Content Loading..."])

(rum/defc character-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:p (str "thing is at " path " that can be gotten with " get-thing)]])

(rum/defc rulebook-page [{:keys [path get-thing put-thing!]}]
  [:.interior
   [:p (str "thing is at " path " that can be gotten with " get-thing)]])



(defmethod page-for nil
  [optmap] (character-page optmap))
(defmethod page-for :home
  [optmap] (character-page optmap))
(defmethod page-for :shell
  [optmap] (shell-page optmap))
(defmethod page-for :character
  [optmap] (character-page optmap))
(defmethod page-for :rulebook
  [optmap] (rulebook-page optmap))