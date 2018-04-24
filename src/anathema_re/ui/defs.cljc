(ns anathema-re.ui.defs
  (:require [rum.core :as rum]
            [anathema-re.data :as data]))

(defmulti form-field-for :field-type)
(defmethod form-field-for nil [_] nil)

(defmulti modal-for :modal-showing)
(defmethod modal-for nil [_] nil)

(defn decode-js-change-event [e]
  (.. e -target -value))

(rum/defc page-of < rum/static
    [{:keys [title subtitle header-content img class sections path]}])

(rum/defc form-seq [{:keys [get-thing put-thing! owner?] :as main-opts}
                    form-field-dec-vec]
  (->> form-field-dec-vec
       (map-indexed
         (fn [n {:keys [label path value] :as a}]
           [:.form-row
            (when label [:label {:for (pr-str path)} label])
            (form-field-for (assoc a
                              :get-thing get-thing
                              :put-thing! put-thing!
                              :owner? owner?
                              :value (or value (get-thing path))))]))
       (into [:.form-of])))


(rum/defc img-field < rum/static
  [{:keys [path value options owner? class get-thing put-thing put-image!] :as opts}]
  [:input.field {:type  :file, ;:value "Select Image",
                 :id (pr-str path)
                 :key   (pr-str path)
                 :accept "image/*"
                 :onChange #(println #?(:cljs (put-image! path (-> % .-target .-files (aget 0)))
                                        :clj "nope"))
                 :class (str class (when (not owner?) " read-only"))}]) ;:value value}])

(rum/defc text-field < rum/static
  [{:keys [path value options owner? class get-thing put-thing!] :as opts}]
  ;(println "Making text field for " path)
  ;(println "Opts are " opts)
  (if owner?
    [:input.field {:type  :text, :value value, :id (pr-str path)
                   :key   (pr-str path)
                   :class (str class (when (not owner?) " read-only"))
                   :onChange #(put-thing! path (decode-js-change-event %))
                   :readOnly (not owner?)}]
    [:span.input-readonly.readonly {:class class} value]))

(defmethod form-field-for :text [n] (text-field n))
(defmethod form-field-for :image [n] (img-field n))

(rum/defc entity-link < rum/static
  [{:keys [img name description key category] :as entity}]
  [:a.entity-link {:href (data/get-navigation-uri-from-path [category key])}
   [:img ;{:src img}]
    {:src (data/modify-imgur-url img :big-square)}]
   [:.entity-info
    [:.name name]
    [:.description description]]])

(rum/defc entity-list
  [{:keys [entity-paths get-thing]}]
  (println "entity paths are " entity-paths)
  (let [entities (map
                     (fn [path]
                       (get-thing path))
                     entity-paths)]
    (println "Entities are " entities)
    (into
      [:.entity-list]
      (map entity-link entities))))

(rum/defc profile-page [{:keys [path get-thing put-thing! put-image!]
                         :as opts}]
    (let [{:keys [key character rulebook img] :as player}
          (get-thing (take 2 path))
          current-player-id (get-thing [:current-player])
          owner? (= key current-player-id)]
      (println "Character vec is " character)
      (println "key is " key "and current player is " current-player-id", so owner is " owner?)
      [:.interior
       [:.section [:h3 "Profile Information"]
        (form-seq
          (assoc opts :owner? owner?)
          [{:field-type :text, :owner? false
            :path       (conj path :name)
            :label      "Display Name"
            :class      "display-name"}
           {:field-type :text, :owner? false
            :path       (conj path :real-name)
            :label      "Real Name"
            :class      "real-name"}
           {:field-type :text, :owner? false
            :path       (conj path :email)
            :label      "Email"
            :class      "email"}
           {:field-type :image, :owner? false
            :path       (conj path :img)
            :value      img
            :label      "Image"
            :class      "image"
            :put-image! put-image!}])
        [:.section [:h3 "Characters"]
         (entity-list {:get-thing    get-thing
                       :entity-paths (map #(into [:character] [(str %)])
                                          character)})]]
       [:.section [:h3 "Rulebooks"]
        (entity-list {:get-thing get-thing
                      :entity-paths (map #(into [:rulebook] [(str %)])
                                         rulebook)})]]))

