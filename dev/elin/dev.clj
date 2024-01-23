(ns elin.dev
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [elin.constant.kind :as e.c.kind]
   [elin.log :as e.log]
   [elin.system :as e.system]
   [malli.dev :as m.dev]))

(def ^:private last-message-store (atom nil))
(def ^:private store-last-message-interceptor
  {:name ::store-last-message-interceptor
   :enter (fn [{:as ctx :keys [message]}]
            (reset! last-message-store message)
            ctx)})

(def config
  (let [f (io/file "dev" "elin" "config.edn")]
    (merge {:server {:host "nvim"
                     :port 12233}
            :interceptor {:manager {e.c.kind/handler [store-last-message-interceptor]}}}
           (if (.exists f)
             (edn/read-string (slurp f))
             {}))))

(def system-map
  (e.system/new-system config))

(defonce sys (atom nil))

(defn start-system
  []
  (when-not @sys
    (e.log/info "Starting elin system")
    (reset! sys (component/start-system system-map))
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

(defn last-message
  []
  @last-message-store)