(ns clojure.tools.namespace.repl-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.test-helpers :as help]
            [clojure.tools.namespace.track :as track]))

(defn reset-repl! []
  (repl/clear)
  (repl/set-refresh-dirs))

(defn reset-repl-fixture [test-fn]
  (reset-repl!)
  (test-fn)
  (reset-repl!))

(use-fixtures :each reset-repl-fixture)

(deftest t-repl-scan-time-component
  (let [before (System/currentTimeMillis)
        scan   (repl/scan {:platform find/clj})
        after  (System/currentTimeMillis)
        time   (::dir/time scan)]
    (is (<= before time after))
    (is (integer? time))))

(deftest t-repl-scan-twice
  (let [dir       (help/create-temp-dir "t-repl-scan")
        other-dir (help/create-temp-dir "t-repl-scan-other")
        main-clj  (help/create-source dir 'example.main :clj '[example.one])
        one-cljc  (help/create-source dir 'example.one :clj)
        _         (repl/set-refresh-dirs dir other-dir)
        scan-1    (repl/scan {:platform find/clj})
        scan-2    (repl/scan {:platform find/clj})
        files-1   (::dir/files scan-1)
        files-2   (::dir/files scan-2)]
    (is (= 2 (count files-1)))
    (is (= files-1 files-2))
    (is (contains? files-1 main-clj))
    (is (contains? files-1 one-cljc))))

(deftest t-repl-scan-after-file-modified
  (let [dir      (help/create-temp-dir "t-repl-scan-after-file-modified")
        main-clj (help/create-source dir 'example.main :clj)
        _        (repl/set-refresh-dirs dir)
        scan-1   (repl/scan {:platform find/clj})
        _        (.setLastModified main-clj (System/currentTimeMillis))
        scan-2   (repl/scan {:platform find/clj})
        files-1  (::dir/files scan-1)
        files-2  (::dir/files scan-2)]
    (is (= 1 (count files-1)))
    (is (= files-1 files-2))
    (is (contains? files-1 main-clj))))

(deftest t-repl-scan-after-dependency-added
  (let [dir       (help/create-temp-dir "t-repl-scan-after-dependency-added")
        _main-clj (help/create-source dir 'example.main :clj)
        _one-clj  (help/create-source dir 'example.one :clj)
        _         (repl/set-refresh-dirs dir)
        scan-1    (repl/scan {:platform find/clj})
        _         (help/create-source dir 'example.main :clj ['example.one])
        scan-2    (repl/scan {:platform find/clj})]
    (is (= {} (:dependencies (::track/deps scan-1))))
    (is (= {} (:dependents (::track/deps scan-1))))
    (is (= {'example.main #{'example.one}} (:dependencies (::track/deps scan-2))))
    (is (= {'example.one #{'example.main}} (:dependents (::track/deps scan-2))))))

(deftest t-repl-scan-after-dependency-removed
  (let [dir       (help/create-temp-dir "t-repl-scan-after-dependency-removed")
        _main-clj (help/create-source dir 'example.main :clj ['example.one])
        _one-clj  (help/create-source dir 'example.one :clj)
        _         (repl/set-refresh-dirs dir)
        scan-1    (repl/scan {:platform find/clj})
        _         (help/create-source dir 'example.main :clj)
        scan-2    (repl/scan {:platform find/clj})]
    (is (= {'example.main #{'example.one}} (:dependencies (::track/deps scan-1))))
    (is (= {'example.one #{'example.main}} (:dependents (::track/deps scan-1))))
    (is (= {} (:dependencies (::track/deps scan-2))))
    (is (= {'example.one #{}} (:dependents (::track/deps scan-2))))))
