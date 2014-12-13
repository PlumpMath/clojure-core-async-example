(ns clojure-core-async-example.channel
  (:require [clojure.core.async :as async :refer [go chan alts!! alts! <! <!! >!! >! close! timeout go-loop]]))

(defn- uuid [] (str (java.util.UUID/randomUUID)))

(def all-channels (atom {}))

(def one-minute (* 60 1000))

(defn- close-channel-after-one-minute [ch ch-id]
  (go
    (<! (timeout one-minute))
    (println (str "Closing channel for id " ch-id))
    (close! ch)
    (and ch-id (swap! all-channels dissoc (keyword ch-id)))))

(defn- create-channel []
  (let [ch (chan)
        ch-id (uuid)
        _ (swap! all-channels assoc (keyword ch-id) ch)
        _ (close-channel-after-one-minute ch ch-id)]
    [ch ch-id]))

(defn- get-channel [request-id]
  ((keyword request-id) @all-channels))

(defn- wait-and-execute [f ms]
  (<!! (timeout ms))
  (f))

(defn- listen-or-timeout [ch ms]
  (alts!! [ch (timeout ms)]))

(def max-wait-time-per-fn 5000)

(def polling-interval 500)

(defn- read-from-channel [ch]
  (let [[v c] (listen-or-timeout ch polling-interval)]
    (or v {:status :no-update})))

(defn- polling-fn-ch [f]
  (let [[temp-ch _] (create-channel)
        max-tries 10]
    (go-loop [i 0]
      (when (< i max-tries)
        (if-let [v (wait-and-execute f polling-interval)]
          (>! temp-ch v)
          (recur (inc i)))))
    temp-ch))

(defn- start-polling [ch & fns]
  (go-loop [all-fns fns]
    (when (not-empty all-fns)
      (let [first-fn-val (-> (polling-fn-ch (first all-fns)) (listen-or-timeout max-wait-time-per-fn) first)]
        (when first-fn-val
          (>! ch first-fn-val)
          (recur (rest all-fns)))))))

(defn fn-a []
  (<!! (timeout 1500))
  {:data "function A value"})

(defn fn-b []
  (<!! (timeout 3500))
  {:data "function B value" :last true})

(defn start []
  (let [[ch request-id] (create-channel)
        _ (start-polling ch fn-a fn-b)]
    {:request_id request-id}))

(defn status [request-id]
  (if-let [ch (get-channel request-id)]
    (read-from-channel ch)
    {:error :invalid-request-id}))



