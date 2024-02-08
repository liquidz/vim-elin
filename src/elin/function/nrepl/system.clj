(ns elin.function.nrepl.system
  (:require
   [clojure.edn :as edn]
   [elin.error :as e]
   [elin.function.nrepl.op :as e.f.n.op]
   [elin.protocol.nrepl :as e.p.nrepl]
   [elin.schema :as e.schema]
   [elin.schema.component :as e.s.component]
   [elin.util.function :as e.u.function]
   [malli.core :as m]))

(def ^:private get-system-info-code
  `(let [user-dir (System/getProperty "user.dir")
         sep (System/getProperty "file.separator")]
     {:user-dir user-dir
      :file-separator sep
      :project-name (-> (.split user-dir sep) seq last)}))

(def ^:private ?SystemInfo
  [:map
   [:user-dir string?]
   [:file-separator string?]
   [:project-name string?]])

(m/=> get-system-info* [:=> [:cat e.s.component/?Nrepl] [:or e.schema/?Error ?SystemInfo]])
(defn- get-system-info*
  [nrepl]
  (e/->> (str get-system-info-code)
         (e.f.n.op/eval!! nrepl)
         (:value)
         (edn/read-string)))

(def get-system-info
  (e.u.function/memoize-by
   (comp e.p.nrepl/current-session first)
   get-system-info*))

(m/=> user-dir [:=> [:cat e.s.component/?Nrepl] [:or e.schema/?Error string?]])
(defn user-dir
  [nrepl]
  (e/-> (get-system-info nrepl)
        (:user-dir)))

(m/=> file-separator [:=> [:cat e.s.component/?Nrepl] [:or e.schema/?Error string?]])
(defn file-separator
  [nrepl]
  (e/-> (get-system-info nrepl)
        (:file-separator)))

(m/=> project-iame [:=> [:cat e.s.component/?Nrepl] [:or e.schema/?Error string?]])
(defn project-iame
  [nrepl]
  (e/-> (get-system-info nrepl)
        (:project-name)))
