(ns webchange.resources.core-web-app
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as s]
    [config.core :refer [env]]))

(def prefix "resources/")
(def public-fold "public/")
(def js-fold (str public-fold "js/compiled/"))
(def css-fold (str public-fold "css/"))
(def img-fold (str public-fold "images/"))
(def fonts-fold (str public-fold "fonts/"))

(defn- remove-prefix
  [s]
  (let [value (str prefix public-fold)
        pos (s/index-of s value)
        len (count value)]
    (str "/" (subs s (+ pos len)))))

(defn- get-files-list
  [dir]
  (if-let [url (io/resource dir)]
    (->> url
         (.getPath)
         (clojure.java.io/file)
         (file-seq)
         (filter #(.isFile %))
         (mapv str)
         (map remove-prefix))))

(defn get-resources-file-path
  []
  "sw/web_app_resources.edn")

(defn generate-app-resources
  []
  (->> ["/manifest.json"
        (get-files-list (str js-fold "app.js"))
        (get-files-list (str js-fold "out/"))
        (get-files-list css-fold)
        (get-files-list img-fold)
        (get-files-list fonts-fold)]
       (flatten)
       (remove nil?)
       (vec)))

(defn- get-resources-list
  []
  (let [path (get-resources-file-path)]
    (-> path io/resource io/reader java.io.PushbackReader. edn/read)))

(defn- get-resources
  []
  (if (env :dev?)
    (generate-app-resources)
    (get-resources-list)))

(defn get-web-app-resources
  []
  {:resources (get-resources)
   :endpoints []})
