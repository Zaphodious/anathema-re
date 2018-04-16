(ns anathema-re.boundary.goog-oauth
  (:require
    [cheshire.core :refer [parse-string]]
    [clj-oauth2.client :as oauth2]))

(def login-uri
  "https://accounts.google.com")

(def google-com-oauth2
  {:authorization-uri (str login-uri "/o/oauth2/auth")
   :access-token-uri (str login-uri "/o/oauth2/token")
   :redirect-uri "http://localhost:8080/authentication/callback"
   :client-id "CLIENT"
   :client-secret "CLIENT-SECRET"
   :access-query-param :access_token
   :scope ["https://www.googleapis.com/auth/userinfo.email"]
   :grant-type "authorization_code"
   :access-type "online"
   :approval_prompt ""})

(def auth-req
  (oauth2/make-auth-request google-com-oauth2))

(defn- google-access-token [request]
  (oauth2/get-access-token google-com-oauth2 (:params request) auth-req))

(defn- google-user-email [access-token]
  (let [response (oauth2/get "https://www.googleapis.com/oauth2/v1/userinfo" {:oauth access-token})]
    (get (parse-string (:body response)) "email")))

;; Redirect them to (:uri auth-req)

;; When they comeback to /authentication/callback (google-user-email)
;=> user's email trying to log in (google-access-token *request*))

