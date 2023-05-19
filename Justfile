set dotenv-load := true
project := "nima-ring"


help:
  @just --list


# Run CLJ tests
test focus=':unit' +opts="":
  @clojure -M:test                                             \
           -m kaocha.runner                                    \
           --reporter kaocha.report/dots                       \
           --focus {{ focus }}                                 \
           {{ opts }}


# Run CLJ tests in watch mode
test-watch: (test ":unit" "--watch")


# Check for outdated deps
outdated:
  @clj -M:outdated
