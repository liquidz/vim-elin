(ns elin.component.server.impl.sexpr
  (:require
   [clojure.core.async :as async]
   [elin.component.server.impl.function :as e.c.s.function]
   [elin.component.server.nvim]
   [elin.component.server.vim]
   [elin.error :as e]
   [elin.protocol.host :as e.p.host]
   [elin.schema :as e.schema]
   [elin.schema.server :as e.s.server]
   [malli.core :as m]))

(m/=> get-top-list-sexpr!* [:=> [:cat e.s.server/?Host int? int?] e.schema/?ManyToManyChannel])
(defn- get-top-list-sexpr!*
  [host lnum col]
  (async/go
    (e/-> (e.c.s.function/request! host "elin#internal#sexp#get_top_list" [lnum col])
          (async/<!)
          (update-keys keyword))))

(m/=> get-list-sexpr!* [:=> [:cat e.s.server/?Host int? int?] e.schema/?ManyToManyChannel])
(defn- get-list-sexpr!*
  [host lnum col]
  (async/go
    (e/-> (e.c.s.function/request! host "elin#internal#sexp#get_list" [lnum col])
          (async/<!)
          (update-keys keyword))))

(m/=> get-single-sexpr!* [:=> [:cat e.s.server/?Host int? int?] e.schema/?ManyToManyChannel])
(defn- get-single-sexpr!*
  [host lnum col]
  (async/go
    (e/-> (e.c.s.function/request! host "elin#internal#sexp#get_expr" [lnum col])
          (async/<!)
          (update-keys keyword))))

(m/=> get-namespace-form!* [:=> [:cat e.s.server/?Host] e.schema/?ManyToManyChannel])
(defn- get-namespace-form!*
  [host]
  (e.c.s.function/request! host "elin#internal#sexp#clojure#get_ns_form" []))

(m/=> replace-namespace-form!* [:=> [:cat e.s.server/?Host string?] e.schema/?ManyToManyChannel])
(defn- replace-namespace-form!*
  [host new-ns-form]
  (e.c.s.function/request! host "elin#internal#sexp#clojure#replace_ns_form" [new-ns-form]))

(extend-protocol e.p.host/ISexpr
  elin.component.server.vim.VimHost
  (get-top-list-sexpr! [this lnum col] (get-top-list-sexpr!* this lnum col))
  (get-list-sexpr! [this lnum col] (get-list-sexpr!* this lnum col))
  (get-single-sexpr! [this lnum col] (get-single-sexpr!* this lnum col))
  (get-namespace-form! [this] (get-namespace-form!* this))
  (replace-namespace-form! [this new-ns-form] (replace-namespace-form!* this new-ns-form))

  elin.component.server.nvim.NvimHost
  (get-top-list-sexpr! [this lnum col] (get-top-list-sexpr!* this lnum col))
  (get-list-sexpr! [this lnum col] (get-list-sexpr!* this lnum col))
  (get-single-sexpr! [this lnum col] (get-single-sexpr!* this lnum col))
  (get-namespace-form! [this] (get-namespace-form!* this))
  (replace-namespace-form! [this new-ns-form] (replace-namespace-form!* this new-ns-form)))
