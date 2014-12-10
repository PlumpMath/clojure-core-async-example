(ns clojure-core-async-example.channel
	(:require [clojure.core.async :as async :refer [go chan alts!! alts! <! <!! >!! >! close! timeout go-loop]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def all-channels (atom {}))

(defn- close-channel-after-one-minute 
	([ch] (close-channel-after-one-minute ch nil))
	([ch ch-id]
		(go
			(<! (timeout (* 60 1000)))
			(println (str "Closing channel for id " ch-id))
			(close! ch)
			(and ch-id (swap! all-channels dissoc (keyword ch-id))))))

(defn- auto-closeable-channel [] 
	(let [ch (chan)]
		(close-channel-after-one-minute ch)
		ch))

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

(defn- read-from-channel [ch]
	(let [[v c] (listen-or-timeout ch 500)]
		(or v {:status :no-update})))

(defn- polling-fn-ch [f ch]
	(let [temp-ch (auto-closeable-channel) max-tries 10]
		(go-loop [i 0] 
			(let [v (wait-and-execute f 500)]
				(cond
					(not (nil? v)) (and (>! ch v) (>! temp-ch v))
					(>= i max-tries) (>! ch {:error :timeout})
					:else (recur (inc i)))))
		temp-ch))

(defn- polling-fn [& fns]
  (fn [ch]
  	(go-loop [all-fns fns]
		(let [first-fn-ch (polling-fn-ch (first all-fns) ch)
			  [first-fn-val c] (listen-or-timeout first-fn-ch 5000)
			  remaining-fns (rest all-fns)]
			(and first-fn-val (not-empty remaining-fns) (recur remaining-fns))))))

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



