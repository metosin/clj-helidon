(ns metosin.nima-ring.static-content
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [metosin.nima-ring.static-content.mime-types :refer [default-mime-types]])
  (:import (java.nio.file Path)
           (io.helidon.common.media.type MediaType)
           (io.helidon.nima.webserver.staticcontent StaticContentService
                                                    StaticContentService$FileBasedBuilder
                                                    StaticContentService$ClassPathBuilder)))


(set! *warn-on-reflection* true)


(defn ->MediaType ^MediaType [media-type]
  (let [[type subtype] (str/split media-type #"\/" 2)]
    (reify MediaType
      (text [_] media-type)
      (type [_] type)
      (subtype [_] subtype))))


(defn- ->Path ^Path [^String path]
  (Path/of path (into-array String [])))


(defn- add-media-types ^StaticContentService$FileBasedBuilder [^StaticContentService$FileBasedBuilder builder mime-types]
  (reduce-kv (fn [^StaticContentService$FileBasedBuilder builder file-ext media-type]
               (.contentType builder ^String file-ext (->MediaType media-type)))
             builder
             mime-types))


(defn- add-welcome-file-name ^StaticContentService$FileBasedBuilder [^StaticContentService$FileBasedBuilder builder index]
  (if index
    (.welcomeFileName builder index)
    builder))


(defn static-files-service
  (^StaticContentService [path] (static-files-service path nil))
  (^StaticContentService [path {:keys [mime-types index]}]
   (assert (and (string? path)
                (.isDirectory (io/file path)))
           (str "path must be a string pointing to a directory: [" path "] (" (type path) ")"))
   (-> (StaticContentService/builder (->Path path))
       (add-media-types (or mime-types default-mime-types))
       (add-welcome-file-name index)
       (.build))))


(defn- add-tmp-dir ^StaticContentService$ClassPathBuilder [^StaticContentService$ClassPathBuilder builder tmp-dir]
  (if tmp-dir
    (.tmpDir builder tmp-dir)
    builder))


(defn static-resources-service
  (^StaticContentService [path] (static-resources-service path nil))
  (^StaticContentService [path {:keys [mime-types
                                       tmp-dir
                                       classloader
                                       index]}]
   (assert (and (string? path)
                (io/resource path))
           (str "path must be a string pointing to a classpath resource: [" path "] (" (type path) ")"))
   (assert (or (nil? tmp-dir)
               (and (string? tmp-dir)
                    (.isDirectory (io/file tmp-dir))))
           (str "tmp-dir must be a string pointing to a directory: [" tmp-dir "]"))
   (assert (or (nil? classloader)
               (instance? ClassLoader classloader))
           (str "classloader must be a java.lang.ClassLoader: [" classloader "] (" (type classloader) ")"))
   (-> (StaticContentService/builder ^String path
                                     (or classloader
                                         (-> (Thread/currentThread)
                                             (.getContextClassLoader))))
       (add-media-types (or mime-types default-mime-types))
       (add-tmp-dir tmp-dir)
       (add-welcome-file-name index)
       (.build))))


(comment
  (static-files-service "src/dev")
  (static-resources-service "dev"))