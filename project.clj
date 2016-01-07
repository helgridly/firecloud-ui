(defn- with-ns [n] (str "org.broadinstitute.firecloud-ui." n))


(defproject org.broadinstitute/firecloud-ui "0.0.1"
  :dependencies
  [
   [dmohs/react "0.2.11"]
   [org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.48"]
   [inflections "0.9.14"]
   [cljsjs/moment "2.9.0-3"]
   ]
  :plugins [[lein-cljsbuild "1.0.6"] [lein-figwheel "0.3.7"] [lein-resource "15.10.2"]]
  :profiles {:dev {:dependencies [[binaryage/devtools "0.4.1"] [devcards "0.2.1-2"]]
                   :cljsbuild
                   {:builds {:client {:source-paths ["src/cljsdev"]
                                      :compiler
                                      {:optimizations :none
                                       :source-map true
                                       :source-map-timestamp true}}}}}
             :figwheel {:cljsbuild
                        {:builds {:client {:source-paths ["src/cljsfigwheel"]
                                           :compiler {:main ~(with-ns "figwheel")}
                                           :figwheel {}}}}}
             :devcards {:cljsbuild
                        {:builds {:client {:compiler {:main ~(with-ns "devcards")}
                                           :figwheel {:devcards true}}}}}
             :deploy {:cljsbuild
                      {:builds {:client {:source-paths ["src/cljsprod"]
                                         :compiler
                                         {:main ~(with-ns "main")
                                          :optimizations :advanced
                                          :pretty-print false}}}}}}
  :cljsbuild {:builds {:client {:source-paths ["src/cljs"]
                                :compiler {:output-dir "target/build"
                                           :asset-path "build"
                                           :output-to "target/compiled.js"}}}}
  :resource {:resource-paths ["src/static"] :skip-stencil [#".*"]})
