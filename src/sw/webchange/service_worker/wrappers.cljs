(ns webchange.service-worker.wrappers
  (:require [webchange.service-worker.logger :as logger]))

;; --- Cache ---

(defn cache-add-all
  [cache requests]
  (.addAll cache requests))

(defn cache-match
  [cache request]
  (.match cache request))

(defn cache-keys
  [cache]
  (.keys cache))

(defn cache-open
  [cache-name]
  (.open js/caches cache-name))

(defn cache-put
  [cache request response]
  (.put cache request response))

(defn cache-delete
  [cache-name]
  (.delete js/caches cache-name))

(defn cache-delete-resource
  [cache request]
  (.delete cache request))

(defn caches-keys
  []
  (.keys js/caches))

;; --- Fetch ---

(defn js-fetch
  ([request]
   (js/fetch request))
  ([request params]
   (js/fetch request params)))

;; --- JSON ---

(defn json-stringify
  [data]
  (.stringify js/JSON data))

;; --- Navigator ---

(defn online?
  []
  (.-onLine js/navigator))

;; --- Promise ---

(defn promise-all
  [promises]
  (->> promises
       (clj->js)
       (js/Promise.all)))

(defn promise-resolve
  ([]
   (promise-resolve nil))
  ([data]
   (js/Promise.resolve data)))

(defn promise-reject
  ([]
   (promise-reject nil))
  ([data]
   (js/Promise.reject data)))

(defn promise
  [handler]
  (js/Promise. handler))

(defn then
  [promise handler]
  (.then promise handler))

(defn catch
  [promise handler]
  (.catch promise handler))

;; --- Body ---

(defn body-json
  [response]
  (.json response))

;; --- Response ---

(defn response-new
  [body headers]
  (js/Response. (json-stringify body) #js{"headers" headers}))

(defn response-clone
  [response]
  (.clone response))

(defn response-headers
  [response]
  (.-headers response))

(defn response-json
  [response]
  (.json response))

(defn response-ok?
  [response]
  (.-ok response))

(defn response-url
  [response]
  (.-url response))

(defn response-url-object
  [response]
  (->> (response-url response)
       (js/URL.)))

(defn response-pathname
  [response]
  (->> response
       (response-url-object)
       (.-pathname)))


;; -- Request ---

(defn request-new
  [url data]
  (js/Request. url data))

(defn request-clone
  [request]
  (.clone request))

(defn request-headers
  [request]
  (.-headers request))

(defn request-json
  [request]
  (.json request))

(defn request-method
  [request]
  (.-method request))

(defn request-url
  [request]
  (.-url request))

(defn request-url-object
  [request]
  (->> (request-url request)
       (js/URL.)))

(defn request-pathname
  [request]
  (->> request
       (request-url-object)
       (.-pathname)))

(defn data->response
  [data]
  (let [headers {"Content-Type" "application/json"}]
    (response-new (clj->js data) (clj->js headers))))

(defn require-status-ok!
  [response]
  (when (or (not (.-ok response))
            (.-redirected response))
    (let [message (str "Response is required to be OK!" (.-url response))]
      (logger/warn message)
      (-> message js/Error. throw)))
  (promise-resolve response))
