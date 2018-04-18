(ns anathema-re.boundary.goog-oauth
  (:require [integrant.core :as ig])
  (:import [com.google.api.client.googleapis.auth.oauth2
            GoogleIdToken
            GoogleIdToken$Payload
            GoogleIdTokenVerifier$Builder
            GoogleIdTokenVerifier]
           [JacksonFactory]
           [NetHttpTransport]
           (com.google.api.client.json.jackson2 JacksonFactory)
           (com.google.api.client.http.javanet NetHttpTransport)))

(defn- google-check [client-id token]
  "Returns GoogleTokenId instance if the token is valid"
  (println "Getting auth for token " token)
  (when token
    (let [jsonFactory (JacksonFactory.)
          transport   (NetHttpTransport.)
          v           (.. (GoogleIdTokenVerifier$Builder. transport jsonFactory)
                          (setAudience (list client-id))
                          (build))]

      (.verify v token))))

(defn- google-verify->map [goog-resp token]
  (let [payload (.getPayload goog-resp)
        valid-from (.getIssuedAtTimeSeconds payload)
        valid-until (.getExpirationTimeSeconds payload)
        the-now (long (/ (. System currentTimeMillis) 1000))]
    {:user-id (.getSubject payload)

     :email (.getEmail payload)
     :email-verified (.getEmailVerified payload)
     :name (.get payload "name")
     :picture-url (.get payload "picture")
     :locale (.get payload "locale")
     :family-name (.get payload "family_name")
     :given-name (.get payload "given_name")
     :valid-from valid-from
     :valid-until valid-until
     :valid-for (- valid-until the-now)
     :token token}))
     ;:string (.toString goog-resp)}))

(defn verify-token [client-id token]
  (let [goog-resp (when (and client-id token)
                    (google-check client-id token))
        resp-map (if goog-resp
                   (google-verify->map goog-resp token)
                   {:user-id nil})]
    resp-map))

(defmethod ig/init-key :anathema-re.boundary/goog-oauth [_ {:keys [environ]}]
  (partial verify-token (:goog-api environ)))