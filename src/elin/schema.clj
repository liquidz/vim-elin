(ns elin.schema
  (:require
   [malli.core :as m])
  (:import
   clojure.core.async.impl.channels.ManyToManyChannel
   (clojure.lang
    Atom
    ExceptionInfo)
   java.util.regex.Pattern))

(defn ?instance
  [klass]
  (m/-simple-schema
   {:type klass
    :pred #(instance? klass %)}))

(def ?File
  (?instance java.io.File))

(def ?NotBytes
  (m/-simple-schema
   {:type ::not-bytes
    :pred #(not (bytes? %))}))

(def ?Error
  (?instance ExceptionInfo))

(def ?ManyToManyChannel
  (?instance ManyToManyChannel))

(def ?Atom
  (?instance Atom))

(def ?Pattern
  (?instance Pattern))

(defn error-or
  [schema]
  [:or ?Error schema])
