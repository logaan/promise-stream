(ns promise-list.test)

(defn test [expected actual]
  (if (not (= expected actual))
    (js/console.log (clj->js expected) (clj->js actual))
    (js/console.log "Pass")))

