(ns elin.interceptor.test
  (:require
   [clojure.string :as str]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.constant.nrepl :as e.c.nrepl]
   [elin.function.nrepl.cider.test :as e.f.n.c.test]
   [elin.function.storage.test :as e.f.s.test]
   [elin.message :as e.message]
   [elin.protocol.host :as e.p.host]
   [elin.protocol.nrepl :as e.p.nrepl]
   [elin.util.map :as e.u.map]
   [exoscale.interceptor :as ix]))

(def ^:private sign-name "error")

(defn- get-failed-tests-cider-nrepl-query
  [failed-results]
  {:ns-query {:exactly (map :ns failed-results)}
   :exactly (->> failed-results
                 (map #(format "%s/%s" (:ns %) (:var %))))})

(defn- get-failed-tests-plain-repl-query
  [failed-results]
  (let [failed-result (->> failed-results
                           (some #(and (:ns %) %)))
        vars (->> failed-results
                  (filter #(= (:ns failed-result) (:ns %)))
                  (map #(symbol (format "#'%s/%s" (:ns %) (:var %)))))]
    {:ns (:ns failed-result)
     :vars vars
     :current-file (:filename failed-result)
     :base-line 0}))

(defn- get-failed-tests-query
  [nrepl failed-results]
  (if (e.p.nrepl/supported-op? nrepl e.c.nrepl/test-var-query-op)
    (get-failed-tests-cider-nrepl-query failed-results)
    (get-failed-tests-plain-repl-query failed-results)))

(def done-test-interceptor
  {:name ::done-test-interceptor
   :kind e.c.interceptor/test
   :leave (-> (fn [{:component/keys [host nrepl session-storage] :keys [response]}]
                (let [{:keys [passed failed]} (->> (e.f.n.c.test/collect-results nrepl response)
                                                   (group-by :result))
                      {:keys [succeeded? summary]} (e.f.n.c.test/summary response)]
                  ;; unsign
                  (if (seq passed)
                    (doseq [var-str (distinct (map :var passed))]
                      (e.p.host/unplace-signs-by host {:name sign-name :group var-str}))
                    (e.p.host/unplace-signs-by host {:name sign-name :group "*"}))
                  ;; sign
                  (doseq [{:as result :keys [lnum]} failed
                          :when lnum]
                    (e.p.host/place-sign host {:name sign-name
                                               :lnum lnum
                                               :file (:filename result)
                                               :group (:var result)}))
                  ;; append results to info buffer
                  (->> failed
                       (mapcat (fn [{:as failed-result :keys [text lnum expected actual]}]
                                 (if (empty? actual)
                                   []
                                   [(format ";; %s%s" text lnum)
                                    (if (seq expected)
                                      (e.u.map/map->str failed-result [:expected :actual :diffs])
                                      actual)
                                    ""])))
                       (str/join "\n")
                       (e.p.host/append-to-info-buffer host))
                  ;; set errors to quickfix list
                  (->> failed
                       (map #(hash-map :filename (:filename %)
                                       :lnum (:lnum %)
                                       :text (cond-> (format "%s/%s" (:ns %) (:var %))
                                               (seq (:text %))
                                               (str ": " (:text %)))
                                       :type "Error"))
                       (e.p.host/set-quickfix-list host))

                  ;; store last failed tests as test query
                  (some->> (get-failed-tests-query nrepl failed)
                           (e.f.s.test/set-last-failed-tests-query session-storage))

                  ;; show summary
                  (e.p.host/append-to-info-buffer host summary)
                  (if succeeded?
                    (e.message/info host summary)
                    (e.message/error host summary))))
              (ix/discard))})
