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
      (if-let [v (wait-and-execute f polling-interval)]
        (>! temp-ch v)
        (cond
          (>= i max-tries) (println "Exhausted number of retries.")
          :else (recur (inc i)))))
    temp-ch))

(defn- polling-fn [& fns]
  (fn [ch]
    (go-loop [all-fns fns]
      (when-let [first-fn (first all-fns)]
        (when-let [first-fn-val (first (listen-or-timeout (polling-fn-ch first-fn) max-wait-time-per-fn))]
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
        poller (polling-fn fn-a fn-b)
        _ (poller ch)
        ]
    {:request_id request-id}))

(defn status [request-id]
  (if-let [ch (get-channel request-id)]
    (read-from-channel ch)
    {:error :invalid-request-id}))



