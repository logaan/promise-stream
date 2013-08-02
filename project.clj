(defproject promise-stream "0.1.0-SNAPSHOT"
  :description "A promise stream serves the same purpose as a blocking lazy
               sequence. Javascript code may not block and so an asynchronous
               alternative is required."
  :url "https://github.com/logaan/promise-stream"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [jayq "2.3.0"]]
  :plugins [[lein-cljsbuild "0.3.0"]] 
  :jar-exclusions  [#"\.(js|html|gitkeep)$"]
  :cljsbuild {
    :builds {
      :dev {:source-paths ["src" "test"]
            :compiler {:output-to "resources/public/js/main.js"}}
      :prod {:source-paths ["src"]
             :compiler {:output-to "resources/public/js/main.min.js"
                        :externs  ["externs/jquery-1.9.js"]
                        :pretty-print false
                        :optimizations :advanced}}}})
