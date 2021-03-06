(ns webchange.interpreter.renderer.scene.app)

(def pixi-app (atom nil))

(defn register-app
  [app]
  (reset! pixi-app app))

(defn add-ticker
  [f]
  (-> @pixi-app
      .-ticker
      (.add f)))

(defn remove-ticker
  [f]
  (-> @pixi-app
      .-ticker
      (.remove f)))

(defn get-renderer
  []
  (.-renderer @pixi-app))
