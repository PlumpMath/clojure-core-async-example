(defproject clojure-core-async-example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
  				 [org.clojure/clojurescript "0.0-2202"]
               	 [compojure "1.1.5"]
                 [jayq "2.4.0"]
                 [hiccup "1.0.4"]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/core.logic "0.8.7"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]]

:plugins [[lein-cljsbuild "1.0.3"]
          [lein-ring "0.8.7"]
          [lein-idea "1.0.1"]]

:main clojure-core-async-example.server
:ring {:handler clojure-core-async-example.server/app}

:source-paths ["src"]
:cljsbuild {
  :builds {
    :main {
          :source-path "src/cljs"
          :compiler {
                    :output-to "resources/public/js/cljs.js"
                    :externs ["resouces/public/js/jquery.js"]
                    :optimizations :simple
                    :pretty-print true}}}}
                 )
