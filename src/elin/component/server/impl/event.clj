(ns elin.component.server.impl.event
  (:require
   [elin.component.server.impl.function :as e.c.s.function]
   [elin.component.server.nvim]
   [elin.component.server.vim]
   [elin.protocol.host :as e.p.host]
   [malli.core :as m]))

(m/=> on-connect* [:=> [:cat e.c.s.function/?IFunction] :nil])
(defn- on-connect*
  [host]
  (e.c.s.function/notify host "elin#ready" []))

(extend-protocol e.p.host/IEvent
  elin.component.server.vim.VimHost
  (on-connect [this] (on-connect* this))

  elin.component.server.nvim.NvimHost
  (on-connect [this] (on-connect* this)))
