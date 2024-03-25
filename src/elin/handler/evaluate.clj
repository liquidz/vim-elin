(ns elin.handler.evaluate
  (:require
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.error :as e]
   [elin.function.nrepl :as e.f.nrepl]
   [elin.function.nrepl.vim :as e.f.n.vim]
   [elin.protocol.host :as e.p.host]
   [elin.protocol.interceptor :as e.p.interceptor]
   [elin.schema.handler :as e.s.handler]
   [elin.util.map :as e.u.map]
   [malli.core :as m]))

(defn- evaluate-interceptor-middleware
  [{:as elin :component/keys [interceptor]}]
  (fn [eval-fn]
    (fn [code options]
      (let [context (-> (e.u.map/select-keys-by-namespace elin :component)
                        (assoc :code code
                               :options options))]
        (:response
         (e.p.interceptor/execute interceptor e.c.interceptor/evaluate context
                                  (fn [{:as ctx :keys [code options]}]
                                    (assoc ctx :response (eval-fn code options)))))))))

(m/=> evaluate [:=> [:cat e.s.handler/?Elin] any?])
(defn evaluate
  [{:as elin :component/keys [nrepl] :keys [message]}]
  (e/let [code (->> message
                    (:params)
                    (first))
          res (e.f.nrepl/eval!! nrepl code {:middleware (evaluate-interceptor-middleware elin)})]
    (:value res)))

(m/=> evaluate-current-top-list [:=> [:cat e.s.handler/?Elin] any?])
(defn evaluate-current-top-list
  [{:as elin :component/keys [host nrepl]}]
  (e/-> {:host host
         :nrepl nrepl
         :options {:middleware (evaluate-interceptor-middleware elin)}}
        (e.f.n.vim/evaluate-current-top-list!!)
        (:response)))

(m/=> evaluate-current-list [:=> [:cat e.s.handler/?Elin] any?])
(defn evaluate-current-list
  [{:as elin :component/keys [host nrepl]}]
  (e/-> {:host host
         :nrepl nrepl
         :options {:middleware (evaluate-interceptor-middleware elin)}}
        (e.f.n.vim/evaluate-current-list!!)
        (:response)))

(m/=> evaluate-current-expr [:=> [:cat e.s.handler/?Elin] any?])
(defn evaluate-current-expr
  [{:as elin :component/keys [host nrepl]}]
  (e/-> {:host host
         :nrepl nrepl
         :options {:middleware (evaluate-interceptor-middleware elin)}}
        (e.f.n.vim/evaluate-current-expr!!)
        (:response)))

(m/=> evaluate-namespace-form [:=> [:cat e.s.handler/?Elin] any?])
(defn evaluate-namespace-form
  [{:as elin :component/keys [host nrepl]}]
  (e/-> {:host host
         :nrepl nrepl
         :options {:middleware (evaluate-interceptor-middleware elin)}}
        (e.f.n.vim/evaluate-namespace-form!!)
        (:response)))

(m/=> load-current-file [:=> [:cat e.s.handler/?Elin] any?])
(defn load-current-file
  [{:component/keys [nrepl host]}]
  (e/let [path (e.p.host/get-current-file-path!! host)]
    (e.f.nrepl/load-file!! nrepl path)
    true))
