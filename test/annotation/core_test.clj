(ns annotation.core-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [kotoba.compiler.core :as compiler]
            [kotoba.compiler.ir :as ir]))

(def source (slurp "src/annotation/core.kotoba"))

(defn- compiler-root []
  (nth (iterate #(.getParent ^java.nio.file.Path %)
                (java.nio.file.Path/of (.toURI (io/resource "kotoba/compiler/core.clj"))))
       4))

(defn- base64 [bytes]
  (.encodeToString (java.util.Base64/getEncoder) bytes))

(deftest source-is-a-real-kotoba-document-library
  (let [checked (compiler/check-source source)
        kir (ir/lower (:hir checked))
        props ["map" [[:id ["string" "urn:annotation:1"]]
                       [:target ["string" "https://example.test/doc"]]]]
        annotation (ir/execute kir 'annotation [props])
        validation (ir/execute kir 'validate [annotation])]
    (is (= :kotoba.hir/v3 (get-in checked [:hir :format])))
    (is (= "map" (first annotation)))
    (is (= ["map" [[:errors ["vector" []]] [:valid? ["bool" true]]]] validation))))

(deftest compiles-to-restricted-javascript-and-real-wasm
  (let [javascript (compiler/compile-source source :js-kotoba-v1)
        wasm (compiler/compile-source source :wasm32-browser-kotoba-v1)
        js-probe
        (shell/sh "node" "--input-type=module" "-e"
                  (str "import('data:text/javascript;base64," (base64 (.getBytes ^String (:source javascript) "UTF-8"))
                       "').then(m=>{const x=m.instantiateKotoba({}),p=['map',[[\":target\",['string','urn:test']]]],a=x.annotation(p);"
                       "const v=x.validate(a);if(a[0]!=='map'||v[1][1][1][1]!==true)process.exit(2)})"))
        wasm-probe
        (shell/sh "node" "--input-type=module" "-e"
                  (str "import(process.argv[1]).then(async m=>{const h=await m.instantiateKotoba(Buffer.from(process.argv[2],'base64'));"
                       "const x=h.instance.exports,p=h.typedValues.document(['map',[[\":target\",['string','urn:test']]]]),a=x.annotation(p);"
                       "if(a[0]!=='map'||x.validate(a)[1][1][1][1]!==true)process.exit(2)})"
                       ".catch(e=>{console.error(e);process.exit(70)})")
                  (.toString (.toUri (.resolve (compiler-root) "runtime/browser-host.mjs")))
                  (base64 ^bytes (:bytes wasm)))]
    (is (string? (:source javascript)))
    (is (pos? (alength ^bytes (:bytes wasm))))
    (is (zero? (:exit js-probe)) (:err js-probe))
    (is (zero? (:exit wasm-probe)) (:err wasm-probe))))
