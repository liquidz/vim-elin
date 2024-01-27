(ns elin.component.interceptor
  (:require
   [com.stuartsierra.component :as component]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.interceptor.connect :as e.i.connect]
   [elin.interceptor.debug :as e.i.debug]
   [elin.interceptor.nrepl :as e.i.nrepl]
   [elin.interceptor.output :as e.i.output]
   [elin.log :as e.log]
   [elin.protocol.interceptor :as e.p.interceptor]
   [exoscale.interceptor :as interceptor]
   [msgpack.clojure-extensions]))

(def ^:private default-interceptors
  [e.i.connect/port-auto-detecting-interceptor
   e.i.connect/output-channel-interceptor
   e.i.output/print-output-interceptor
   e.i.nrepl/eval-ns-interceptor
   e.i.nrepl/normalize-path-interceptor])

(def ^:private dev-interceptors
  [e.i.debug/interceptor-context-checking-interceptor
   e.i.debug/nrepl-debug-interceptor])

(defrecord Interceptor
  [manager]
  component/Lifecycle
  (start [this]
    (e.log/debug "Interceptor component: Started")
    this)
  (stop [this]
    (e.log/info "Interceptor component: Stopped")
    (dissoc this :manager))

  e.p.interceptor/IInterceptor
  (add-interceptor! [_ kind interceptor]
    (swap! manager update kind #(conj (or % []) interceptor)))
  (remove-interceptor! [_ interceptor]
    (swap! manager update-vals (fn [vs] (vec (remove #(= % interceptor) vs)))))
  (remove-interceptor! [_ kind interceptor]
    (swap! manager update kind (fn [vs] (vec (remove #(= % interceptor) vs)))))
  (execute [_ kind context]
    (->> (or (get @manager kind) [])
         (interceptor/execute context)))
  (execute [this kind context terminator]
    (let [interceptors (concat
                        (or (get @manager e.c.interceptor/all) [])
                        (or (get @manager kind) []))
          terminator' {:name ::terminator
                       :enter terminator}
          context' (assoc context
                          :elin/interceptor this
                          :elin/kind kind)]
      (interceptor/execute context' (concat interceptors [terminator'])))))

(defn new-interceptor
  [{:as config :keys [develop?]}]
  (let [initial-manager (->> (concat default-interceptors
                                     (when develop? dev-interceptors)
                                     (get-in config [:interceptor :interceptors]))
                             (group-by :kind))]
    (map->Interceptor {:manager (atom initial-manager)})))
