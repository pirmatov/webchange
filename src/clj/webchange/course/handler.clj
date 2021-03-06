(ns webchange.course.handler
  (:require [compojure.api.sweet :refer [api context GET POST PUT DELETE defroutes routes swagger-routes]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [resource-response response redirect]]
            [webchange.course.core :as core]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [clojure.tools.logging :as log]
            [webchange.common.handler :refer [handle current-user current-school validation-error]]
            [webchange.validation.validate :refer [validate]]
            [webchange.auth.core :as auth]
            [webchange.auth.website :as website]
            [spec-tools.data-spec :as ds]
            [schema.core :as s]
            [config.core :refer [env]]
            [compojure.api.middleware :as mw]))

(defn handle-save-scene
  [course-slug scene-name request]
  (let [owner-id (current-user request)
        save (fn [data] (core/save-scene! course-slug scene-name data owner-id))]
    (-> request
        :body
        :scene
        save
        handle)))

(defn handle-save-course
  [course-slug request]
  (let [owner-id (current-user request)
        save (fn [data] (core/save-course! course-slug data owner-id))]
    (-> request
        :body
        :course
        save
        handle)))

(defn handle-save-course-info
  [course-id request]
  (let [owner-id (current-user request)
        save (fn [data] (core/save-course-info! (Integer/parseInt course-id) data))]
    (-> request
        :body
        save
        handle)))

(defn handle-restore-course-version
  [version-id request]
  (let [owner-id (current-user request)]
    (-> (core/restore-course-version! (Integer/parseInt version-id) owner-id)
        handle)))

(defn handle-restore-scene-version
  [version-id request]
  (let [owner-id (current-user request)]
    (-> (core/restore-scene-version! (Integer/parseInt version-id) owner-id)
        handle)))

(defn handle-localize-course
  [course-id data request]
  (let [website-user-id (:user-id data)
        language (:language data)
        owner-id (some-> website-user-id
                         website/get-user-by-id
                         auth/get-user-id-by-website!)]
    (if owner-id
      (-> (core/localize course-id {:lang language :owner-id owner-id :website-user-id website-user-id})
          handle)
      (handle [false {:message "User not found"}]))))

(s/defschema Course {:id s/Int :name s/Str :slug s/Str :image-src (s/maybe s/Str) :url s/Str :lang (s/maybe s/Str)})
(s/defschema Translate {:user-id s/Int :language s/Str})
(s/defschema EditorTag {:id s/Int :name s/Str})
(s/defschema EditorAsset {:id s/Int :path s/Str :thumbnail-path s/Str :type (s/enum "background" "surface" "decoration")})
(s/defschema CharacterSkin {:name s/Str :width s/Num :height s/Num :skins [s/Str] :animations [s/Str]})

(defn character-skins []
  (response (core/find-all-character-skins)))

(defn editor-assets [tag type]
  (response (core/editor-assets tag type)))

(defn find-all-tags []
  (response (core/find-all-tags)))

(def website-api-routes
  (api
  (swagger-routes {:ui   "/api-docs"
                   :data {:info     {:title       "TabSchools API"}
                          :tags     [{:name "course", :description "Course APIs"}]}})
  (context "/api/courses" []
    :tags ["course"]
    ;should go before general "/api/courses/:course-slug" to be accessible
    (GET "/editor/assets" []
      :summary "Return list of available assets"
      :query-params [{type :- s/Str nil}, {tag :- s/Int nil}]
      :return [EditorAsset]
      (editor-assets tag type)
      )
    (GET "/editor/tags" []
      :summary "Return list of available assets"
      :return [EditorTag]
      (find-all-tags)
      )
    (GET "/editor/character-skin" []
      :summary "Return skins available for objects"
      :return [CharacterSkin]
      (character-skins)
      )
    (GET "/available" []
      :return [Course]
      :summary "Returns all available courses"
      (-> (core/get-available-courses) response))
    (POST "/:course-id/translate" request
      :path-params [course-id :- s/Int]
      :return Course
      :body [translate Translate]
      :summary "Starts course translation"
      (handle-localize-course course-id translate request))
    (GET "/by-website-user/:website-user-id" request
      :path-params [website-user-id :- s/Int]
      :return [Course]
      :summary "Returns courses by website user id"
      (-> (core/get-courses-by-website-user website-user-id) response)))))



(defroutes course-routes
    (GET "/api/courses/:course-slug" [course-slug] (-> course-slug core/get-course-data response))
    (GET "/api/courses/:course-slug/scenes/:scene-name" [course-slug scene-name] (-> (core/get-scene-data course-slug scene-name) response))
    (POST "/api/courses/:course-slug/scenes/:scene-name" [course-slug scene-name :as request]
      (handle-save-scene course-slug scene-name request))
    (POST "/api/courses/:course-slug" [course-slug :as request]
      (handle-save-course course-slug request))

    (GET "/api/courses/:course-slug/info" [course-slug]
      (-> course-slug core/get-course-info response))
    (PUT "/api/courses/:course-id/info" [course-id :as request]
      (handle-save-course-info course-id request))

    (GET "/api/courses/:course-slug/versions" [course-slug] (-> course-slug core/get-course-versions response))
    (GET "/api/courses/:course-slug/scenes/:scene-name/versions" [course-slug scene-name] (-> (core/get-scene-versions course-slug scene-name) response))
    (POST "/api/course-versions/:version-id/restore" [version-id :as request]
      (handle-restore-course-version version-id request))
    (POST "/api/scene-versions/:version-id/restore" [version-id :as request]
      (handle-restore-scene-version version-id request)))
