(ns clojure-core-async-example.client
    (:require [jayq.core :as jq :refer [$ append ajax inner html $deferred done resolve pipe on]]
    		  [jayq.util :refer [log]]
    		  [cljs.core.async :refer [<! >! chan close! put! alts!]])

  	(:require-macros [cljs.core.async.macros :refer [go]]
  					 [jayq.macros :refer [let-ajax]]))

(defn data-from-event [event]
  (-> event .-currentTarget $ .data (js->clj :keywordize-keys true)))

(defn click-chan [selector]
  (let [rc (chan)]
    (on ($ "body") :click selector {}
        (fn [e]
          (jq/prevent e)
          (let-ajax [result {:url "/start" :dataType :json :type :post}]
          	(put! rc result))))
    rc))

(def $statuses ($ :#statuses))

(defn clear-statuses []
	(html $statuses ""))

(defn show-data [d]
	(append $statuses (str "<div>" (or (.-status d) (.-error d) (.-data d)) "</div>")))

(defn start-requesting-status [request-id]
	(let [ch (chan)
		  _ (request-status request-id ch)
		  _ (go (loop []
				(log "Waiting on status updates")
				(let [data (<! ch)
					  _ (log data)
					  _ (show-data data)
					  not-last (not (.-last data))
					  no-errors (not (.-error data))]
					  (if (and no-errors not-last)
						(do 
							(request-status request-id ch)
							(recur))
						(close! ch)))))]))

(defn request-status [request-id ch]
	(let-ajax [result {:url (str "/status/" request-id) :dataType :json}]
    	(put! ch result)))


(go 
	(log "Waiting for you to click")	
	(let [ch (click-chan "#clickhere")
		  data (<! ch)
		  _ (clear-statuses)
		  _ (close! ch)
		  request-id (.-request_id data)]
		(start-requesting-status request-id)))

