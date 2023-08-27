(ns metosin.nima-ring.examples.sse.server
  (:require [metosin.nima-ring.nima-server :as nima])
  (:import (java.nio.file Path)
           (io.helidon.nima.webserver WebServer
                                      WebServerConfig$Builder)
           (io.helidon.nima.webserver.http HttpRouting
                                           Handler
                                           ServerRequest
                                           ServerResponse)
           (io.helidon.nima.webserver.staticcontent StaticContentService)))

(comment



  (defn handler [_]
    {:status  200
     :headers {}
     :body    "Hi"})

  (let [builder (HttpRouting/builder)]
    (.any builder (nima/ring-handler->nima-handler handler))
    (.register builder "/" (into-array
                            java.util.function.Supplier
                            [(-> (StaticContentService/builder (Path/of "src/examples/metosin/nima_ring/examples/sse/public"
                                                                        (into-array String [])))
                                 (.welcomeFileName "index.html")
                                 (.build))]))
    (.build builder))

  StaticContentService
  io.helidon.nima.webserver.staticcontent.StaticContentService
  ;
  )

; Routing.builder()
;       .register("/pictures", StaticContentSupport.create(Paths.get("/some/WEB/pics"))) // <1>
;       .register("/", StaticContentSupport.builder("/static-content") // <2>
;                                   .welcomeFileName("index.html") // <3>
;                                   .build());

; .register("/pics", StaticContentSupport.create("/static/pictures"))


(defn handler [req]
  {:status  200
   :headers {"content-type" "text/plain"}
   :body    "Hi"})


(defn -main [& args]
  (nima/nima-server handler {:port 8080}))
