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


# Make a release, creates a tag and pushes it
@release version +message:
  git diff --quiet || (echo "Working directory is dirty"; false)
  git tag -a {{ version }} -m "{{ message }}"
  git push --tags
  bash -c 'echo -n "SHA: "'
  git rev-parse --short {{ version }}^{commit}


@current-release:
  #!/usr/bin/env bash
  TAG=$(git tag --sort=-taggerdate | head -n 1)
  SHA=$(git rev-parse --short ${TAG}^{commit})
  echo ":git/tag \"${TAG}\""
  echo ":git/sha \"${SHA}\""
