(ns release
  (:require [clojure.string :as str]
            [babashka.process :as p]))


(defn current []
  (let [tag (-> (p/shell {:out :string} "git tag --sort=-taggerdate")
                :out
                (str/split-lines)
                (first))
        sha (-> (p/shell {:out :string} (format "git rev-parse --short %s^{commit}" tag))
                :out
                (str/trim))]
    [tag sha]))


(defn new-release [version message]
  (when (str/blank? version)
    (println "version is required")
    (System/exit 1))
  (when (str/blank? message)
    (println "message is required")
    (System/exit 1))
  (when-not (-> (p/process "git diff --quiet")
                (deref)
                :exit
                (zero?))
    (println "error: Working directory is dirty")
    (System/exit 1))
  (p/shell "git tag" "-a" version "-m" message)
  (p/shell "git push --tags")
  (current))
