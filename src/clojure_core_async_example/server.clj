(ns clojure-core-async-example.server
  (:require [clojure-core-async-example.channel :as channel]
            [compojure.handler :as handler]
            [compojure.route :as route])
  (:use [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.util.response :only [response content-type]]
        [hiccup.core]
        [compojure.core]))


(defn view-layout [& content]
  (html
    [:head [:meta {:http-equiv "Content-type"
                   :content "text/html; charset=utf-8"}]
     [:title "Wth Clojurescript!!"]]
    [:body content]))

(defn view-content []
  (view-layout
    [:h2 "Clojurescript with core async"]
    [:p {:id "clickhere"} "Click me"]
    [:div {:id "statuses"}]
    [:script {:src "/js/jquery.js"}]
    [:script {:src "/js/cljs.js"}]))

(defroutes main-routes
  (GET "/" []
    (view-content))

  (POST "/start" []
    {:body (channel/start)})

  (GET "/status/:id" [id]
    {:body (channel/status id)})

  (route/resources "/"))

(def app
  (->
    (handler/api main-routes)
    (wrap-json-body)
    (wrap-json-response)))