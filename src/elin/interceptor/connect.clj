(ns elin.interceptor.connect
  (:require
   [clojure.core.async :as async]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.protocol.host :as e.p.host]
   [elin.protocol.interceptor :as e.p.interceptor]
   [elin.util.file :as e.u.file]
   [elin.util.map :as e.u.map]
   [exoscale.interceptor :as ix]))

(def port-auto-detecting-interceptor
  {:name ::port-auto-detecting-interceptor
   :kind e.c.interceptor/connect
   :enter (fn [{:as ctx :component/keys [host] :keys [hostname port]}]
            (if (and hostname port)
              ctx
              (let [;; TODO error handling
                    cwd (async/<!! (e.p.host/get-current-working-directory! host))
                    nrepl-port-file (e.u.file/find-file-in-parent-directories cwd ".nrepl-port")
                    hostname' (or hostname "localhost")
                    port' (some-> nrepl-port-file
                                  (slurp)
                                  (Long/parseLong))]
                (assoc ctx :hostname hostname' :port port'))))})

(def output-channel-interceptor
  {:name ::output-channel-interceptor
   :kind e.c.interceptor/connect
   :leave (-> (fn [{:as ctx :component/keys [interceptor] :keys [client]}]
                (async/go-loop []
                  (let [ch (get-in client [:connection :output-channel])
                        output (async/<! ch)]
                    (when output
                      (-> ctx
                          (e.u.map/select-keys-by-namespace :component)
                          (assoc :output output)
                          (->> (e.p.interceptor/execute interceptor e.c.interceptor/output)))
                      (recur)))))
              (ix/when #(:client %))
              (ix/discard))})

(def connected-interceptor
  {:name ::connected-interceptor
   :kind e.c.interceptor/connect
   :leave (fn [{:as ctx :component/keys [interceptor]}]
            (-> ctx
                (e.u.map/select-keys-by-namespace :component)
                (assoc :autocmd-type "BufEnter")
                (->> (e.p.interceptor/execute interceptor e.c.interceptor/autocmd)))
            ctx)})
