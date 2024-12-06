(ns clojure.tools.namespace.repl-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.repl :as repl]))

(deftest t-repl-scan-time-component
  (let [before (System/currentTimeMillis)
        scan   (repl/scan {:platform find/clj})
        after  (System/currentTimeMillis)
        time   (::dir/time scan)]
    (is (<= before time after))
    (is (integer? time))))
