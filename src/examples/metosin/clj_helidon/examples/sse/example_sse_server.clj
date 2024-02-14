(ns metosin.clj-helidon.examples.sse.example-sse-server
  (:require [metosin.clj-helidon.server :as nima]
            [metosin.clj-helidon.static-content :as static-content]
            [metosin.clj-helidon.sse :as sse]))


(defn messages-handler [req]
  (println "SSE: new client")
  (let [emitter (sse/sse-emitter req)]
    (try
      (loop [n 0]
        (emitter {:comment "test message"
                  :id      (str "#" n)
                  :name    "message"
                  :data    (str "hello " n)})
        (Thread/sleep 1000)
        (recur (inc n)))
      (catch Exception e
        (if (-> e .getCause .getMessage (= "Broken pipe"))
          (println "SSE: Client closed connection")
          (println "SSE: Got exception" e))
        (emitter)))))


(def public-files "src/examples/metosin/nima_ring/examples/sse/public")


(defn -main [& _args]
  (nima/create-server [[:get "/ping" (constantly {:status 200
                                                  :body   {:message "pong"}})]
                       [:get "/messages" messages-handler]
                       [:service "/" (static-content/static-files-service public-files {:index "index.html"})]]
                      {:port 8080}))



(comment


  (def server (-main))


  (nima/port server)
  ;; => 8080


  (require '[clj-http.client :as client])


  (-> (client/get "http://localhost:8080/ping" {:headers {"accept" "application/json"}})
      ((juxt (comp #(get % "Content-Type") :headers) :body)))
  ;; => ["application/json; charset=UTF-8" 
  ;;     "{\"message\":\"pong\"}"]


  (-> (client/get "http://localhost:8080/ping" {:headers {"accept" "application/edn"}})
      ((juxt (comp #(get % "Content-Type") :headers) :body)))
  ;; => ["application/edn; charset=UTF-8" 
  ;;     "{:message \"pong\"}"]


  ;; $ curl http://localhost:8080/
  ;; <!DOCTYPE html>
  ;; <html>
  ;; <head>
  ;;   <title>Example SSE client with clj-helidon</title>
  ;; ...


  ;; $ curl -N -H "Accept:text/event-stream" http://localhost:8080/messages
  ;; :test message
  ;; id:#0
  ;; event:message
  ;; data:hello 0
  ;;
  ;; :test message
  ;; id:#1
  ;; event:message
  ;; data:hello 1
  ;;
  ;; :test message
  ;; id:#2
  ;; event:message
  ;; data:hello 2  
  ;;
  ;; ...

  (nima/shutdown server)
  ;
  )
