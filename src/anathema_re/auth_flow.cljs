(ns anathema-re.auth-flow
  (:require [anathema-re.ui :as ui]
            [rum.core :as rum]
            [anathema-re.data :as data]
            [anathema-re.data-layer :as dl]
            [clojure.core.async :as async]))

(def auth-opts-map {:supportedAuthMethods      ["https://accounts.google.com"]
                    :supportedIdTokenProviders [{:uri      "https://accounts.google.com"
                                                 :clientId js/sitekey}]})

(defn retrieve [g-yolo]
  (println "Starting YOLO Service")
  (let [return-chan (async/promise-chan)
        retrieve-promise (.retrieve g-yolo (clj->js auth-opts-map))]
    (println "retrieving!")
    (-> retrieve-promise
        ;(.catch (fn [a] (println "sign-in failed, " a) a))
        ;(.catch (fn [a] (.hint g-yolo (clj->js (assoc auth-obj :context "signUp")))))
        (.then #(async/put! return-chan %)
               #(async/put! return-chan false)))
    return-chan))

(defn hint [input-chan g-yolo]
  (let [return-chan (async/promise-chan)]
    (println "Hinting!")
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
    (println "Making the button thing!")
    (async/go
      (let [input-result (async/<! input-chan)
            total-result (if input-result input-result
                                          (async/<! button-chan))]
        (async/>! return-chan total-result)))
    return-chan))

(defn transform-auth-response [response]
  (async/go
    (println "response is " (pr-str (js->clj response)))))

(defn transform-auth-response-chan [input-chan]
  (println "transorming button thing!")
  (async/take! input-chan transform-auth-response))

(defn init-auth [yolo]
  (-> (retrieve yolo) (hint yolo) transform-auth-response-chan))

(comment "First, let's define the workflow.

1. Retrieve auth from Google's servers.")