(ns webchange.interpreter.renderer.state.scene
  (:require
    [re-frame.core :as re-frame]
    [webchange.interpreter.renderer.state.db :refer [path-to-db]]
    [webchange.interpreter.renderer.scene.components.wrapper-interface :as w]
    [webchange.logger :as logger]))

(re-frame/reg-event-fx
  ::init
  (fn [{:keys [db]} [_]]
    {:db (assoc-in db (path-to-db [:objects]) (atom {}))}))

(re-frame/reg-fx
  :add-scene-object
  (fn [{:keys [scene-objects object-wrapper]}]
    (let [object-name (:name object-wrapper)]
      (when (contains? @scene-objects object-name)
        (-> (str "Object with name " object-name " already exists.") js/Error. throw))
      (swap! scene-objects assoc object-name object-wrapper))))

(re-frame/reg-event-fx
  ::register-object
  (fn [{:keys [db]} [_ object-wrapper]]
    {:add-scene-object {:scene-objects  (get-in db (path-to-db [:objects]))
                        :object-wrapper object-wrapper}}))

(defn get-scene-object
  [db name]
  (let [objects @(get-in db (path-to-db [:objects]))]
    (get objects name)))

;; Change object events

(re-frame/reg-event-fx
  ::set-scene-object-state
  (fn [{:keys [_]} [_ object-name state]]
    (let [available-actions {:set-position   [:x :y]
                             :set-scale      [:scale :scale-x :scale-y]
                             :set-visibility [:visible]}
          execute-actions (->> available-actions
                               (reduce (fn [result [action params]]
                                         (assoc result action (select-keys state params)))
                                       {})
                               (filter (fn [[_ params]] (-> params empty? not))))
          not-handled-params (clojure.set/difference (->> state (keys) (set))
                                                     (->> available-actions (vals) (apply concat) (set)))]
      (when-not (empty? not-handled-params)
        (logger/warn "not-processed-params:" (clj->js not-handled-params)))
      {:dispatch [::change-scene-object object-name execute-actions]})))

(re-frame/reg-event-fx
  ::change-scene-object
  (fn [{:keys [db]} [_ object-name actions]]
    (let [objects (get-in db (path-to-db [:objects]))
          object-wrapper (get @objects object-name)]
      (reduce (fn [result [action params]]
                (assoc result action [object-wrapper params]))
              {}
              actions))))

(re-frame/reg-fx
  :add-filter
  (fn [[object-wrapper filter-data]]
    (w/add-filter object-wrapper filter-data)))

(re-frame/reg-fx
  :set-position
  (fn [[object-wrapper position]]
    (w/set-position object-wrapper position)))

(re-frame/reg-fx
  :set-scale
  (fn [[object-wrapper scale]]
    (w/set-scale object-wrapper scale)))

(re-frame/reg-fx
  :set-visibility
  (fn [[object-wrapper {:keys [visible]}]]
    (w/set-visibility object-wrapper visible)))

(re-frame/reg-fx
  :set-value
  (fn [[object-wrapper value]]
    (w/set-value object-wrapper value)))

(re-frame/reg-fx
  :set-text
  (fn [[object-wrapper text]]
    (w/set-text object-wrapper text)))

(re-frame/reg-fx
  :set-src
  (fn [[object-wrapper src]]
    (w/set-src object-wrapper src)))
