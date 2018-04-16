(ns anathema-re.ui
  (:require [rum.core :as rum]
            [clojure.core.async :as async]
            [anathema-re.data :as data]
            #?(:cljs [anathema-re.google-signin-button :as gapi])))

(defmulti page-for #(-> % :path first))

#?(:clj
    (rum/defc loading-page [{:keys [path get-thing] :as optsmap}]
      (let [{:keys [name img] :as thing} (get-thing path)]
        [:.interior
         [:img {:src (if img img "https://i.imgur.com/Ij692NO.jpg")}]
         [:p (str "Loading page for " name)]])))

#?(:cljs
   (rum/defc signin-button [{:keys [api-key put-thing!]}]
     (gapi/signin-button :client-id api-key)))

(rum/defc app-core < rum/reactive
  [{:keys [path get-thing reactive-atom entity api-key user-info-get put-thing!] :as optsmap}]
  (when reactive-atom (rum/react reactive-atom))
  ;(println "thing is definitely " (get-thing path) " at " path)
 ; (println "atomo is " reactive-atom)
  (let [{:keys [name category]
         :as entity-to-render} (get-thing path)
        page (page-for optsmap)]
    ;(println "thing is " entity-to-render)
    [:#app-frame
     [:.page-title [:h1 name]]
     #?(:cljs (gapi/signin-button :client-id api-key
                                  :on-success (fn [google-user]
                                                (-> google-user (.getAuthResponse) js->clj println))
                                  :on-failure println))
     [:#menu [:ul [:li [:i.material-icons.menu-icon "apps"] [:span.label "Home"]]
              [:li [:i.material-icons.menu-icon "settings"] [:span.label "Settings"]]
              [:li [:i.material-icons.menu-icon "storage"] [:span.label "My Profile"]]
              [:li [:i.material-icons.menu-icon "face"] [:span.label "My Characters"]]
              [:li [:i.material-icons.menu-icon "book"] [:span.label "My Rulebooks"]]]]
     [:#content
      [:.page page]]]))

(rum/defc homepage [{:keys [path get-thing api-key] :as optsmap}]
  [:html {:lang "en" :class "home"}
   [:head
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:meta {:name "google-signin-client_id" :content api-key}]
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
    [:script {:src "https://smartlock.google.com/client"}]
    [:script {:src "/js/main.js"}]
    [:script {:src "https://apis.google.com/js/platform.js" :async true :defer true}]]])


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