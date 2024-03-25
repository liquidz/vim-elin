(ns elin.component.clj-kondo
  (:require
   [babashka.pods :as b.pods]
   [clojure.core.async :as async]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [elin.error :as e]
   [elin.protocol.clj-kondo :as e.p.clj-kondo]
   [elin.protocol.host :as e.p.host]
   [elin.util.file :as e.u.file]
   [taoensso.timbre :as timbre]))

(defn- get-project-root-directory
  [host]
  (e/let [cwd (async/<!! (e.p.host/get-current-working-directory! host))
          root (or (e.u.file/get-project-root-directory cwd)
                   (e/not-found))]
    (.getAbsolutePath root)))

(defn- get-cache-file-path
  [user-dir]
  (.getAbsolutePath
   (io/file (e.u.file/get-cache-directory)
            (str (str/replace user-dir "/" "_")
                 ".edn"))))

(def clj-kondo-available?
  (try
    (b.pods/load-pod "clj-kondo")
    true
    (catch Exception _ false)))

(when clj-kondo-available?
  (require '[pod.borkdude.clj-kondo :as clj-kondo]))

(defrecord CljKondo
  [;; COMPONENTS
   lazy-host
   ;; CONFIGS
   config
   ;; PARAMS
   analyzing?-atom
   analyzed-atom]
  component/Lifecycle
  (start [this]
    (timbre/info "CljKondo component: Started")
    (assoc this
           :analyzing?-atom (atom false)
           :analyzed-atom (atom nil)))

  (stop [this]
    (timbre/info "CljKondo component: Stopped")
    (dissoc this :analyzing?-atom :analyzed-atom))

  e.p.clj-kondo/ICljKondo
  (analyze [this]
    (cond
      (not clj-kondo-available?)
      (async/go (e/unavailable {:message "clj-kondo is unavailable"}))

      (e.p.clj-kondo/analyzing? this)
      (async/go (e/busy {:message "clj-kondo is already analyzing"}))

      :else
      (do (reset! analyzing?-atom true)
          (async/thread
            (try
              #_{:clj-kondo/ignore [:unresolved-namespace]}
              (e/let [project-root-dir (get-project-root-directory lazy-host)
                      res (clj-kondo/run! {:lint [project-root-dir]
                                           :config config})
                      cache-path (get-cache-file-path project-root-dir)]
                (spit cache-path (pr-str res))
                (reset! analyzed-atom res))
              (finally
                (reset! analyzing?-atom false)))))))

  (restore [this]
    (cond
      (not clj-kondo-available?)
      (async/go (e/unavailable {:message "clj-kondo is unavailable"}))

      (e.p.clj-kondo/analyzing? this)
      (async/go (e/busy {:message "clj-kondo is already analyzing"}))

      :else
      (do (reset! analyzing?-atom true)
          (async/thread
            (try
              (e/let [project-root-dir (get-project-root-directory lazy-host)
                      cache-file (get-cache-file-path project-root-dir)
                      analyzed (with-open [r (io/reader cache-file)]
                                 (edn/read (java.io.PushbackReader. r)))]
                (reset! analyzed-atom analyzed))
              (catch java.io.FileNotFoundException  ex
                (e/not-found {:message (ex-message ex)}))
              (finally
                (reset! analyzing?-atom false)))))))

  (analyzing? [_]
    @analyzing?-atom)

  (analyzed? [_]
    (some? @analyzed-atom))

  (analysis [this]
    (when (and clj-kondo-available?
               (e.p.clj-kondo/analyzed? this))
      (:analysis @analyzed-atom))))

(defn new-clj-kondo
  [config]
  (map->CljKondo (or (:clj-kondo config) {})))
