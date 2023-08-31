(ns metosin.nima-ring.path-matcher
  (:import (io.helidon.common.http PathMatcher
                                   PathMatchers)))



(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; Path matcher:
;; ========================================================================================
;;


(defn path-matcher-exact "Exact match path matcher." ^PathMatcher [^String exact]
  (PathMatchers/exact exact))


(defn path-matcher-prefix "Prefix match path matcher." ^PathMatcher [^String prefix]
  (PathMatchers/prefix prefix))


(defn path-matcher-pattern "Pattern match path matcher." ^PathMatcher [^String pattern]
  (PathMatchers/pattern pattern))


(defn path-matcher-any "Path matcher matching any path." ^PathMatcher []
  (PathMatchers/any))


(defn path-matcher
  "Create a path matcher from a path pattern. This method will analyze the pattern 
   and return appropriate path matcher. The following characters mark this as:
     - ends with /* and no other   - prefix match
     - {...}                       - pattern with a named parameter
     - \\*                           - pattern glob
     - \\                           - special characters (regexp)"
  ^PathMatcher [path]
  (if (instance? PathMatcher path)
    path
    (PathMatchers/create path)))
