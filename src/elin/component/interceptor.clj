(ns elin.component.interceptor
  (:require
   [com.stuartsierra.component :as component]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.interceptor.autocmd]
   [elin.interceptor.connect]
   [elin.interceptor.nrepl]
   [elin.interceptor.output]
   [elin.log :as e.log]
   [elin.protocol.config :as e.p.config]
   [elin.protocol.interceptor :as e.p.interceptor]
   [elin.schema.interceptor :as e.s.interceptor]
   [exoscale.interceptor :as interceptor]
   [malli.core :as m]
   [msgpack.clojure-extensions]))

(def ^:private config-key :interceptor)

(defn- resolve-interceptor [lazy-writer sym]
  (try
    (deref (requiring-resolve sym))
    (catch Exception ex
      (e.log/warning lazy-writer "Failed to resolve interceptor" {:symbol sym :ex ex})
      nil)))

(defn- valid-interceptor?
  [x]
  (m/validate e.s.interceptor/?Interceptor x))

(defrecord Interceptor
  [lazy-writer     ; LazyWriter component
   plugin          ; Plugin component
   includes
   excludes
   interceptor-map]
  component/Lifecycle
  (start [this]
    (let [exclude-set (set excludes)
          grouped-interceptors (->> (concat (or includes [])
                                            (or (get-in plugin [:loaded-plugin :interceptors]) []))
                                    (distinct)
                                    (remove #(contains? exclude-set %))
                                    (keep #(resolve-interceptor lazy-writer %))
                                    (group-by valid-interceptor?))
          interceptor-map (group-by :kind (get grouped-interceptors true))]
      (when-let [invalid-interceptors (seq (get grouped-interceptors false))]
        (println "Invalid interceptors:" invalid-interceptors)
        (e.log/warning lazy-writer "Invalid interceptors:" invalid-interceptors))
      (e.log/debug lazy-writer "Interceptor component: Started")
      (assoc this :interceptor-map interceptor-map)))
  (stop [this]
    (e.log/debug lazy-writer "Interceptor component: Stopped")
    (dissoc this :interceptor-map))

  e.p.interceptor/IInterceptor
  (execute [this kind context]
    (e.p.interceptor/execute this kind context identity))
  (execute [this kind context terminator]
    (let [interceptors (concat
                        (or (get interceptor-map e.c.interceptor/all) [])
                        (or (get interceptor-map kind) []))
          terminator' {:name ::terminator
                       :enter terminator}
          context' (assoc context
                          :elin/interceptor this
                          :elin/kind kind)]
      (try
        (interceptor/execute context' (concat interceptors [terminator']))
        (catch Exception ex
          (e.log/error lazy-writer "Failed to intercept:" (ex-message ex))))))

  e.p.config/IConfigure
  (configure [this config]
    (let [{:keys [includes excludes]} (get config config-key)
          exclude-set (->> (or excludes [])
                           (map keyword)
                           (set))
          grouped (->> (or includes [])
                       (keep #(resolve-interceptor lazy-writer %))
                       (group-by valid-interceptor?))
          include-map (group-by :kind (get grouped true))
          configured (reduce-kv
                      (fn [accm kind interceptors]
                        (assoc accm kind (concat (get accm kind []) interceptors)))
                      interceptor-map
                      include-map)
          configured (update-vals configured
                                  (fn [interceptors]
                                    (remove #(contains? exclude-set (:name %)) interceptors)))]
      (assoc this :interceptor-map configured))))

(defn new-interceptor
  [config]
  (map->Interceptor (or (get config config-key) {})))
