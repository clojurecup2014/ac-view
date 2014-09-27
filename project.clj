(defproject ac-view "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [crate "0.2.5"]
                 [domina "1.0.3-SNAPSHOT"]
                 [hiccup "1.0.5"]
                 [ring "1.3.1"]
                 ;[phaser-cljs "0.1.0-SNAPSHOT"] ; It needs `lein install`
                 ]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.11"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {:builds {:dev {:id "dev"
                             :source-paths ["src/cljs"]
                             :compiler {:output-to "resources/public/cljs.js"
                                        :optimizations :whitespace
                                        :pretty-print true}
                              :jar true}
                       :prod {:id "prod"
                              :source-paths ["src/cljs"]
                              :compiler {:output-to "resources/public/cljs.js"
                                         ;:externs ["externs/hoge_externs.js"]
                                         :optimizations :simple ; :advanced
                                         :pretty-print false}
                              :jar true}
                       }}
  ;; This is for dummy-server
  :main ac-view.server
  :ring {:handler ac-view.server/app
         :init ac-view.server/init
         :port 8002
         })

