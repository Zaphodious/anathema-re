(ns anathema-re.auth-flow
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [clojure.core.async :as async]
            [cognitect.transit :as transit]))

(def auth-opts-map {:supportedAuthMethods      ["https://accounts.google.com"]
                    :supportedIdTokenProviders [{:uri      "https://accounts.google.com"
                                                 :clientId js/sitekey}]})

(defn retrieve [g-yolo]
  ;(println "Starting YOLO Service")
  (let [return-chan (async/promise-chan)
        retrieve-promise (.retrieve g-yolo (clj->js auth-opts-map))]
    ;(println "retrieving!")
    (-> retrieve-promise
        ;(.catch (fn [a] (println "sign-in failed, " a) a))
        ;(.catch (fn [a] (.hint g-yolo (clj->js (assoc auth-obj :context "signUp")))))
        (.then #(async/put! return-chan %)
               #(async/put! return-chan false)))
    return-chan))

(defn hint [input-chan g-yolo]
  (let [return-chan (async/promise-chan)]
    ;(println "Hinting!")
    (async/take! input-chan
                 (fn [result]
                   (if result
                     (async/put! return-chan result)
                     (-> g-yolo (.hint (clj->js (assoc auth-opts-map :context "signUp")))
                         (.then #(async/put! return-chan %)
                                #(async/put! return-chan false))))))
    return-chan))

(defn button-into-flow [input-chan button-chan]
  (let [return-chan (async/promise-chan)]
    ;(println "Making the button thing!")
    (async/go
      (let [input-result (async/<! input-chan)
            total-result (if input-result input-result
                                          (async/<! button-chan))]
        (async/>! return-chan total-result)))
    return-chan))

(defn transform-auth-response [response]
  (when response
    (async/go
      (let [deconstruct-response (js->clj response :keywordize-keys true)
            token (or (:idToken deconstruct-response) (:id_token deconstruct-response))]
        ;(println "keys is " (pr-str (:idToken deconstruct-response)))
        (-> (js/fetch "/api/player/me.transit" (clj->js {:headers {"token" token}}))
            (.then #(.text %))
            (.then #(transit/read (transit/reader :json) %))
            (.then #(reset! dl/auth-cache %))
            (.then #(swap! dl/page-temp-state (fn [a] (assoc a :current-player (:key %))))))
      ;(println "auth-cache is " dl/auth-cache)
        ;(.reload js/location true)
        (swap! dl/auth-cache (fn [a] (merge a deconstruct-response)))
        (dl/auth-cache-to-temp-state!)))))

(defn transform-auth-response-chan [input-chan]
  ;(println "transorming button thing!")
  (async/take! input-chan transform-auth-response))

(defn confirm-auth-valid []
  (let [{:keys [valid-until]} @dl/auth-cache
        the-now (/ (.getTime (js/Date.)) 1000)]
    ;(println "valid until " valid-until "while now is " the-now)
    (and valid-until (> valid-until the-now))))

(defn refresh-page [_])
  ;(.reload js/location true))

(defn init-auth [yolo]
  (when (not (confirm-auth-valid))
    (-> (retrieve yolo) (hint yolo) transform-auth-response-chan)))

      ;transform-auth-response-chan))

(comment "First, let's define the workflow.

1. Retrieve auth from Google's servers.")