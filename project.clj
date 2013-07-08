(defproject promise-list "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [jayq "2.3.0"]]
  :plugins [[lein-cljsbuild "0.3.0"]] 
  :cljsbuild {
    :builds {
      :dev {:source-paths ["src" "test"]
            :compiler {:output-to "resources/public/js/main.js"}}
      :prod {:source-paths ["src"]
             :compiler {:output-to "resources/public/js/main.min.js"
                        :externs  ["externs/jquery-1.9.js"]
                        :pretty-print false
                        :optimizations :advanced}}}})
