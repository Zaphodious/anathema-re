(defproject anathema-re "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [com.cognitect/transit-clj "0.8.303"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [duct/core "0.6.2"]
                 [duct/module.logging "0.3.1"]
                 [duct/module.web "0.6.4"]
                 [duct/module.cljs "0.3.2"]
                 [duct/server.http.http-kit "0.1.2"]
                 [rum "0.11.2"]
                 [environ "1.1.0"]
                 [garden "1.3.5"]
                 [com.rpl/specter "1.1.0"]
                 [gzip-util "0.1.0-SNAPSHOT"]
                 [com.novemberain/monger "3.1.0"]
                 [com.cemerick/url "0.1.1"]
                 [jstrutz/hashids "1.0.1"]
                 [com.tristanstraub/cljs-google-signin "0.1.0-SNAPSHOT"]
                 [com.fasterxml.jackson.core/jackson-core "2.9.5"] ;Repl crashes without it. 🤷
                 [bk/ring-gzip "0.3.0"]
                 [stuarth/clj-oauth2 "0.3.2"]]
  :plugins [[duct/lein-duct "0.10.6"]
            [lein-environ "1.1.0"]]
  :main ^:skip-aot anathema-re.main
  :uberjar-name  "anathema-re-standalone.jar"
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :env {:mongodb-uri "mongodb%3A%2F%2Flocalhost%3A27017%2Fanathema"
                        :masterkey "devkey42"
                        :goog-api "29298066663-qjl14dig7cbd0n128pq5r2cctc7pqrrq.apps.googleusercontent.com"}
                  :dependencies   [[integrant/repl "0.2.0"]
                                   [eftest "0.4.1"]
                                   [kerodon "0.9.0"]]}})
