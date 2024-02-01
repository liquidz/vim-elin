(ns elin.handler.evaluate
  (:require
   [elin.error :as e]
   [elin.function.nrepl.op :as e.f.n.op]
   [elin.function.vim :as e.f.vim]
   [elin.function.vim.sexp :as e.f.v.sexp]
   [elin.handler :as e.handler]))

(defn- evaluation*
  [{:component/keys [nrepl]}
   code & [options]]
  (e/let [options (reduce-kv (fn [accm k v]
                               (if v
                                 (assoc accm k v)
                                 accm))
                             {:nrepl.middleware.print/stream? 1}
                             options)
          res (e.f.n.op/eval!! nrepl code options)]
    (:value res)))

;; TODO status: ["namespace-not-found" "done" "error"]

(defmethod e.handler/handler* :evaluate
  [{:as elin :keys [message]}]
  (->> message
       (:params)
       (first)
       (evaluation* elin)))

(defmethod e.handler/handler* :evaluate-current-top-list
  [{:as elin :component/keys [writer]}]
  (e/let [{:keys [lnum col]} (e.f.vim/get-cursor-position!! writer)
          ns-str (e.f.v.sexp/get-namespace!! writer)
          path (e.f.vim/get-full-path!! writer)
          {:keys [code lnum col]} (e.f.v.sexp/get-top-list!! writer lnum col)]
    (evaluation* elin code {:line lnum
                            :column col
                            :ns ns-str
                            :file path})))

(defmethod e.handler/handler* :evaluate-current-list
  [{:as elin :component/keys [writer]}]
  (e/let [{:keys [lnum col]} (e.f.vim/get-cursor-position!! writer)
          ns-str (e.f.v.sexp/get-namespace!! writer)
          path (e.f.vim/get-full-path!! writer)
          {:keys [code lnum col]} (e.f.v.sexp/get-list!! writer lnum col)]
    (evaluation* elin code {:line lnum
                            :column col
                            :ns ns-str
                            :file path})))

(defmethod e.handler/handler* :evaluate-current-expr
  [{:as elin :component/keys [writer]}]
  (e/let [{:keys [lnum col]} (e.f.vim/get-cursor-position!! writer)
          ns-str (e.f.v.sexp/get-namespace!! writer)
          path (e.f.vim/get-full-path!! writer)
          {:keys [code lnum col]} (e.f.v.sexp/get-expr!! writer lnum col)]
    (evaluation* elin code {:line lnum
                            :column col
                            :ns ns-str
                            :file path})))
