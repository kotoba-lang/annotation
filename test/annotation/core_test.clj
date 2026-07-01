(ns annotation.core-test
  (:require [annotation.core :as anno]
            [clojure.test :refer [deftest is]]))

(deftest builds-annotation
  (let [a (anno/annotation {:id "urn:annotation:1"
                            :motivation "commenting"
                            :body (anno/text-body "hello")
                            :target "https://example.test/doc"})]
    (is (= "Annotation" (:type a)))
    (is (= "TextualBody" (get-in a [:body :type])))
    (is (:valid? (anno/validate a)))))

(deftest builds-specific-target
  (let [t (anno/specific-resource {:source "https://example.test/doc"
                                   :selector (anno/text-position-selector 1 4)})]
    (is (= "SpecificResource" (:type t)))
    (is (= 4 (get-in t [:selector :end])))))
