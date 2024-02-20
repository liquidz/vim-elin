(ns elin.dev
  (:require
   [com.stuartsierra.component :as component]
   [elin.config :as e.config]
   [elin.log :as e.log]
   [elin.system :as e.system]
   [malli.dev :as m.dev]))

(def server-config (atom {}))
(defonce sys (atom nil))

(defn initialize
  [{:keys [host port]}]
  (reset! server-config {:server {:host host :port port}}))

(defn start-system
  []
  (when-not @sys
    (e.log/info "Starting elin system")
    (let [config (e.config/load-config "." @server-config)
          system-map (e.system/new-system config)]
      (reset! sys (component/start-system system-map)))
    ::started))

(defn stop-system
  []
  (when @sys
    (e.log/info "Stopping elin system")
    (component/stop-system @sys)
    (reset! sys nil)
    ::stopped))

(defn start
  []
  (start-system)
  (m.dev/start!))

(defn stop
  []
  (stop-system)
  (m.dev/stop!))

(defn go
  []
  (stop)
  (start))
