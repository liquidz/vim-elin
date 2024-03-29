(ns elin.handler.test
  (:require
   [clojure.string :as str]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.constant.nrepl :as e.c.nrepl]
   [elin.error :as e]
   [elin.function.evaluate :as e.f.evaluate]
   [elin.function.nrepl.cider :as e.f.n.cider]
   [elin.function.nrepl.test :as e.f.n.test]
   [elin.protocol.interceptor :as e.p.interceptor]
   [elin.protocol.nrepl :as e.p.nrepl]
   [elin.schema.handler :as e.s.handler]
   [elin.util.map :as e.u.map]
   [malli.core :as m]
   [rewrite-clj.zip :as r.zip]))

(defn- extract-multi-method-name
  [code]
  (let [zloc (-> (r.zip/of-string code)
                 (r.zip/down))]
    (when (contains? #{'defmulti 'defmethod} (r.zip/sexpr zloc))
      (some-> zloc
              (r.zip/next)
              (r.zip/sexpr)
              (str)))))

(m/=> run-test-under-cursor [:=> [:cat e.s.handler/?Elin] any?])
(defn run-test-under-cursor
  [{:as elin :component/keys [interceptor]}]
  (e/let [{:keys [code response options]} (e.f.evaluate/evaluate-current-top-list elin)
          {ns-str :ns} options
          var-name (or (some->> (extract-multi-method-name code)
                                (str ns-str "/"))
                       (str/replace (:value response) #"^#'" ""))
          context (-> (e.u.map/select-keys-by-namespace elin :component)
                      (assoc :ns ns-str
                             :line (:line options)
                             :column (:column options)
                             :file (:file options)
                             :vars [var-name]))]
    (e.p.interceptor/execute
     interceptor e.c.interceptor/test context
     (fn [{:as ctx :component/keys [nrepl]}]
       (if (e.p.nrepl/supported-op? nrepl e.c.nrepl/test-var-query-op)
         ;; cider-nrepl
         (let [query {:ns-query {:exactly [(:ns ctx)]}
                      :exactly (:vars ctx)}]
           (assoc ctx :response (e.f.n.cider/test-var-query!! nrepl query)))

         ;; plain
         (let [query {:ns (:ns ctx)
                      :vars (:vars ctx)
                      :current-file (:file ctx)
                      :base-line (:line ctx)}]
           (assoc ctx :response (e.f.n.test/test-var-query!! nrepl query))))))))
