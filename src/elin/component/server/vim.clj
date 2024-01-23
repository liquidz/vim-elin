(ns elin.component.server.vim
  "https://vim-jp.org/vimdoc-en/channel.html#channel-use"
  (:require
   [cheshire.core :as json]
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [elin.log :as e.log]
   [elin.protocol.rpc :as e.p.rpc]
   [elin.util.id :as e.u.id]
   [msgpack.clojure-extensions])
  (:import
   java.io.EOFException))

(defrecord VimMessage
  [host message response-manager]
  e.p.rpc/IMessage
  (request? [_]
    (and (sequential? message)
         (int? (first message))
         (not= 0 (first message))
         (not (contains? @response-manager (first message)))))

  (response? [_]
    (and (sequential? message)
         (= 2 (count message))
         (int? (first message))
         (contains? @response-manager (first message))))

  (parse-message [this]
    (cond
      (e.p.rpc/response? this)
      (let [[id result] message]
        {:id id
         :error nil
         :result result})

      (e.p.rpc/request? this)
      (let [[id [method params]] message]
        {:id id
         :method (keyword method)
         :params params})

      ;; notify
      :else
      (let [[_ [method params callback]] message]
        {:method (keyword method)
         :params params
         :callback callback}))))

(defrecord VimWriter
  [output-stream response-manager]
  e.p.rpc/IWriter
  (request! [_ [method :as content]]
    (let [id (cond
               (= "call" method) (nth content 3)
               (= "expr" method) (nth content 2))
          maybe-ch (when id (async/chan))]
      (when (and id maybe-ch)
        (swap! response-manager assoc id maybe-ch))
      (e.log/info "FIXME request!" (pr-str content))
      (json/generate-stream content (io/writer output-stream))
      maybe-ch))

  (notify! [_ content]
    (e.log/info "FIXME notify! " (pr-str content))
    (json/generate-stream content (io/writer output-stream)))

  (response! [this error result]
    (when-let [id (:id (e.p.rpc/parse-message this))]
      (e.log/info "FIXME" (pr-str [id (or error result)]))
      (-> [id (or error result)]
          (json/generate-stream  (io/writer output-stream)))))

  (flush! [_]
    (.flush output-stream))

  e.p.rpc/IFunction
  (call-function [this method params]
    (e.p.rpc/request! this ["call" method params (e.u.id/next-id)]))

  (echo-text [this text]
    (e.p.rpc/notify! this ["ex" "echo" (format "'%s'" text)]))

  (echo-message [this text]
    (e.p.rpc/echo-message this text "Normal"))
  (echo-message [this text highlight]
    (e.p.rpc/notify! this ["call" "elin#internal#echom" [text highlight]])))

(defn start-server
  [{:keys [host server-socket on-accept stop-signal]}]
  (let [response-manager (atom {})]
    ;; Client accepting loop
    (loop []
      (try
        (with-open [client-sock (.accept server-socket)]
          (let [output-stream (.getOutputStream client-sock)
                input-stream (io/reader (.getInputStream client-sock))]
            ;; Client message reading loop
            (loop []
              (let [[raw-msg ch] (async/alts!! [stop-signal
                                                (async/thread
                                                  (try
                                                    (json/parse-stream input-stream)
                                                    (catch Exception ex ex)))])]
                (when (and (not= stop-signal ch)
                           raw-msg
                           (not (instance? Exception raw-msg)))
                  (e.log/debug "Vim server received message:" (pr-str raw-msg))
                  (on-accept {:message (map->VimMessage {:host host
                                                         :message raw-msg
                                                         :response-manager response-manager})
                              :writer (map->VimWriter {:output-stream output-stream
                                                       :response-manager response-manager})})
                  (when-not (.isClosed client-sock)
                    (recur))))))
          (e.log/debug "Client socket is closed"))
        (catch EOFException _
          nil)
        (catch Exception ex
          (e.log/debug "Client connection is closed" (ex-message ex))))
      (when-not (.isClosed server-socket)
        (recur)))))