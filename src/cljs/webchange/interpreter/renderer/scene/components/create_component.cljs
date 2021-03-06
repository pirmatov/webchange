(ns webchange.interpreter.renderer.scene.components.create-component
  (:require
    [re-frame.core :as re-frame]
    [webchange.interpreter.renderer.state.scene :as state]
    [webchange.interpreter.renderer.scene.components.index :refer [components]]
    [webchange.interpreter.renderer.scene.components.props-utils :refer [get-props get-object-props]]
    [webchange.interpreter.renderer.scene.components.dragging :refer [enable-drag!]]
    [webchange.interpreter.renderer.scene.components.wrapper-interface :as w]
    [webchange.interpreter.renderer.scene.components.editor-mode :as editor-mode]))

(def default-object-props {:draggable   {}
                           :on-drag-end {}
                           :visible     {:default true}
                           :rotation    {:default 0}
                           :opacity     {}})

(defn- init-display-object!
  [{object :object :as wrapper} props props-to-exclude]
  (let [default-props (apply dissoc default-object-props props-to-exclude)
        {:keys [draggable on-drag-end visible rotation opacity]} (get-object-props props default-props)]
    (when (some? draggable)
      (when draggable
        (enable-drag! object on-drag-end)))
    (when (some? visible)
      (w/set-visibility wrapper visible))
    (when (some? rotation)
      (w/set-rotation wrapper rotation))
    (when (some? opacity)
      (w/set-opacity wrapper opacity))))

(defn- get-component
  [type]
  (let [{:keys [constructor default-props] :as component} (get components type)]
    (when (nil? constructor)
      (-> (str "Object with type <" type "> can not be drawn because it is not defined") js/Error. throw))
    (when (nil? default-props)
      (-> (str "Default props for <" type "> are not defined") js/Error. throw))
    component))

(defn- enable-mode
  [mode props]
  (case mode
    "editor" (editor-mode/enable props)
    props))

(defn create-component
  ([props]
   (create-component "play" props))
  ([mode {:keys [type] :as props}]
   (let [{:keys [constructor default-props]} (get-component type)
         modified-props (enable-mode mode props)
         component-wrapper (->> (get-props type modified-props default-props {:exclude-check (keys default-object-props)})
                                (constructor))]
     (init-display-object! component-wrapper modified-props (keys default-props))
     (when (= type "group")
       (let [group-instance (:container component-wrapper)
             children (:children modified-props)]
         (doseq [child children]
           (create-component mode (assoc child :parent group-instance)))))
     (when (nil? component-wrapper)
       (-> (str "Constructor for <" type "> did not return component wrapper") js/Error. throw))
     (re-frame/dispatch [::state/register-object component-wrapper]))))
