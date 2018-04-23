(ns anathema-re.sw
  (:require [hireling.core :as hireling]))

(println "The service worker is now installed.")

(hireling/start-service-worker!
  {:version 1
   :app-name "anathema"
   :precaching [{:url "/shell.html" :revision 1}
                {:url "/api/rulebook/0.transit" :revision 1}
                {:url "/" :revision 1}
                {:url "/sitekey.js" :revision 1}
                ;{:url "/js/main.js" :revision 1}
                {:url "/rand/precached.txt" :revision 1}
                {:url "/style/main.css" :revision 1}]
   :navigation-route {; URL to be called from the cache. Should be identical to one
                      ; provided in :precaching.
                      :url "/shell.html"
                      ; Optional. The name of the cache the asset is in. If falsy, the
                      ; precache name will be used.
                      :cache-name nil
                      ; Optional. If the navigation path matches a regex listed here, the
                      ; cached asset  will not be returned. Overrides the whitelist.
                      :blacklist [#"/api/"]
                      ; Optional. Similar to the blacklist. If a route matches here and is not
                      ; canceled by the blacklist, the cached asset will be returned.
                      :whitelist [#"/character/"
                                  #"/player/"
                                  #"/rulebook/"]}
   :precache-routing-opts {:directoryIndex ""}
   :cache-routes [{:strategy :cache-first
                   :max-entries 2
                   :max-age-seconds (* 60 60 2)
                   :route #"/style/"
                   :get :GET}
                  {:strategy :stale-while-revalidate
                   :route #"/js/"
                   :max-entries 1000}
                  {:strategy :network-first
                   :route #".transit"
                   :max-entries 15}
                  {:strategy :cache-first
                   :route #"/img/"
                   :max-entries 200}
                  {:strategy :cache-first
                   :route #"imgur"
                   :max-entries 200}
                  {:strategy :cache-first
                   :route #"/fonts/"
                   :max-entries 10}]})
