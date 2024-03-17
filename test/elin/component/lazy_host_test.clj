(ns elin.component.lazy-host-test
  (:require
   [clojure.core.async :as async]
   [clojure.test :as t]
   [com.stuartsierra.component :as component]
   [elin.protocol.host :as e.p.host]
   [elin.protocol.host.rpc :as e.p.h.rpc]
   [elin.system :as e.system]
   [elin.test-helper :as h]))

(t/use-fixtures :once h/malli-instrument-fixture)

(t/deftest new-lazy-host-test
  (let [{:as sys :keys [lazy-host]} (-> (e.system/new-system)
                                        (select-keys [:lazy-host])
                                        (component/start-system))
        wrote (atom [])
        host (h/test-host {:handler #(do (swap! wrote conj %)
                                         "OK")})]
    (try
      (e.p.h.rpc/notify! lazy-host ["before"])
      (e.p.host/set-host! lazy-host host)
      (e.p.h.rpc/notify! lazy-host ["after"])

      (t/is (= [[2 "after"]] @wrote))

      (async/<!! (async/timeout 200))

      (t/is (= 2 (count @wrote)))
      (t/is (= #{[2 "before"] [2 "after"]}
               (set @wrote)))

      (finally
        (component/stop-system sys)))))
