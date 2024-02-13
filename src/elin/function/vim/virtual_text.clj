(ns elin.function.vim.virtual-text
  (:refer-clojure :exclude [set])
  (:require
   [elin.function.vim  :as e.f.vim]))

(defn set
  [writer text]
  (let [text (str "=> " text)]
    (e.f.vim/notify writer "elin#internal#virtual_text#set" [text])))

(defn clear-all
  [writer]
  (e.f.vim/notify writer "elin#internal#virtual_text#clear" []))
