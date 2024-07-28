(ns elin.schema.component
  (:require
   [elin.protocol.clj-kondo :as e.p.clj-kondo]
   [elin.protocol.interceptor :as e.p.interceptor]
   [elin.protocol.nrepl :as e.p.nrepl]
   [elin.protocol.storage :as e.p.storage]
   [elin.schema :as e.schema]))

(def ?LazyHost
  [:map
   [:host-store e.schema/?Atom]])

(def ?Interceptor
  (e.schema/?protocol e.p.interceptor/IInterceptor))

(def ?Nrepl
  (e.schema/?protocol e.p.nrepl/IClientManager
                      e.p.nrepl/IClient
                      e.p.nrepl/IConnection))

(def ?CljKondo
  (e.schema/?protocol e.p.clj-kondo/ICljKondo))

(def ?Storage
  (e.schema/?protocol e.p.storage/IStorage))
