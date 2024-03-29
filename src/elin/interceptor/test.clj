(ns elin.interceptor.test
  (:require
   [clojure.string :as str]
   [elin.constant.interceptor :as e.c.interceptor]
   [elin.function.nrepl.cider.test :as e.f.n.c.test]
   [elin.function.vim.info-buffer :as e.f.v.info-buffer]
   [elin.message :as e.message]
   [elin.protocol.host :as e.p.host]
   [elin.util.map :as e.u.map]
   [exoscale.interceptor :as ix]))

(def ^:private sign-name "error")

(def done-test-interceptor
  {:name ::done-test-interceptor
   :kind e.c.interceptor/test
   :leave (-> (fn [{:component/keys [host nrepl] :keys [response]}]
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
                       (e.f.v.info-buffer/append host))
                  ;; TODO quickfix
                  ;; call iced#qf#set(errors)
                  (comment nil)

                  ;; show summary
                  (e.f.v.info-buffer/append host summary)
                  (if succeeded?
                    (e.message/info host summary)
                    (e.message/error host summary))))
              (ix/discard))})
