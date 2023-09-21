(ns metosin.nima-ring.static-content
  (:require [clojure.java.io :as io]
            [metosin.nima-ring.default-mime-types :refer [default-mime-types]]
            [metosin.nima-ring.media-type :refer [media-type]])
  (:import (java.nio.file Path)
           (io.helidon.nima.webserver.staticcontent StaticContentService
                                                    StaticContentService$FileBasedBuilder
                                                    StaticContentService$ClassPathBuilder)))


(set! *warn-on-reflection* true)


(defn- ->Path ^Path [path]
  (cond
    (instance? Path path) path
    (instance? java.io.File path) (.toPath ^java.io.File path)
    (string? path) (Path/of path (into-array String []))))


(defn- set-media-types ^StaticContentService$FileBasedBuilder [^StaticContentService$FileBasedBuilder builder mime-types]
  (reduce-kv (fn [^StaticContentService$FileBasedBuilder builder file-ext mime-type]
               (.contentType builder ^String file-ext (media-type mime-type)))
             builder
             mime-types))


(defn- set-welcome-file-name ^StaticContentService$FileBasedBuilder [^StaticContentService$FileBasedBuilder builder ^String index]
  (if index
    (.welcomeFileName builder index)
    builder))


(defn- set-path-mapper ^StaticContentService$FileBasedBuilder [^StaticContentService$FileBasedBuilder builder ^java.util.function.Function path-mapper]
  (if path-mapper
    (.pathMapper builder path-mapper)
    builder))


(defn html5-history-path-mapper
  (^java.util.function.Function [] (html5-history-path-mapper nil))
  (^java.util.function.Function [index]
   (let [index (or index "/index.html")]
     (reify java.util.function.Function
       (apply [_ path]
         (if (re-matches #"(^|.*/)[^.]*" path)
           index
           path))))))


(defn static-files-service
  (^StaticContentService [path] (static-files-service path nil))
  (^StaticContentService [path {:keys [mime-types index path-mapper]}]
   (assert (and (string? path)
                (.isDirectory (io/file path)))
           (str "path must be a string pointing to a directory: [" path "] (" (type path) ")"))
   (-> (StaticContentService/builder (->Path path))
       (set-media-types (or mime-types @default-mime-types))
       (set-welcome-file-name index)
       (set-path-mapper (if (= path-mapper :html5) (html5-history-path-mapper index) path-mapper))
       (.build))))


(defn- set-tmp-dir ^StaticContentService$ClassPathBuilder [^StaticContentService$ClassPathBuilder builder tmp-path]
  (if tmp-path
    (.tmpDir builder (->Path tmp-path))
    builder))


(defn static-resources-service
  (^StaticContentService [path] (static-resources-service path nil))
  (^StaticContentService [path {:keys [mime-types
                                       index
                                       tmp-dir
                                       classloader
                                       path-mapper]}]
   (assert (and (string? path)
                (io/resource path))
           (str "path must be a string pointing to a classpath resource: [" path "] (" (type path) ")"))
   (assert (or (nil? tmp-dir)
               (string? tmp-dir)
               (instance? java.io.File tmp-dir)
               (instance? Path tmp-dir))
           (str "tmp-dir must be a string, java.io.File or java.nio.file.Path: [" tmp-dir "] (" (type path) ")"))
   (assert (or (nil? classloader)
               (instance? ClassLoader classloader))
           (str "classloader must be a java.lang.ClassLoader: [" classloader "] (" (type classloader) ")"))
   (-> (StaticContentService/builder ^String path
                                     (or classloader
                                         (-> (Thread/currentThread)
                                             (.getContextClassLoader))))
       (set-media-types (or mime-types @default-mime-types))
       (set-tmp-dir (->Path tmp-dir))
       (set-welcome-file-name index)
       (set-path-mapper (if (= path-mapper :html5) (html5-history-path-mapper index) path-mapper))
       (.build))))


(comment
  (static-files-service "src/dev")
  (static-resources-service "dev"))