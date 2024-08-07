(ns elin.function.clj-kondo-test
  (:require
   [clojure.test :as t]
   [elin.function.clj-kondo :as sut]
   [elin.test-helper :as h]))

(t/use-fixtures :once h/malli-instrument-fixture)
(t/use-fixtures :once h/warn-log-level-fixture)

(t/deftest namespaces-by-alias-test
  (let [c (h/test-clj-kondo)]
    (t/is (= '[elin.util.id]
             (sut/namespaces-by-alias c 'e.u.id)))
    (t/is (empty? (sut/namespaces-by-alias c 'non-existing-alias)))))

(t/deftest requiring-namespaces-test
  (let [c (h/test-clj-kondo)]
    (t/is (= '[malli.core]
             (sut/requiring-namespaces c "elin.util.id")))
    (t/is (empty? (sut/requiring-namespaces c "non-existing-ns")))))

(t/deftest lookup-test
  (let [c (h/test-clj-kondo)]
    (t/testing "not qualified symbol"
      (t/is (= {:ns "elin.function.clj-kondo"
                :name "lookup"
                :file "src/elin/function/clj_kondo.clj"
                :arglists-str ""}
               (-> (sut/lookup c "elin.function.clj-kondo" "lookup")
                   (dissoc :line :column)))))

    (t/testing "qualified symbol with alias"
      (t/is (= {:ns "elin.error"
                :name "not-found"
                :file "src/elin/error.clj"
                :arglists-str ""}
               (-> (sut/lookup c "elin.function.clj-kondo" "e/not-found")
                   (dissoc :line :column)))))

    (t/testing "qualified symbol without alias"
      (t/is (= {:ns "elin.error"
                :name "not-found"
                :file "src/elin/error.clj"
                :arglists-str ""}
               (-> (sut/lookup c "elin.function.clj-kondo" "elin.error/not-found")
                   (dissoc :line :column)))))

    (t/testing "full namespace"
      (t/is (= {:name "elin.error"
                :file "src/elin/error.clj"
                :ns ""
                :arglists-str ""}
               (-> (sut/lookup c "elin.function.clj-kondo" "elin.error")
                   (dissoc :line :column)))))))
