(ns zerol.db-test
  (:require [clojure.test :refer [is are deftest testing]]
            [zerol.db :as db]))

(deftest eav->map
  (is (= [#:customer {:name "Joe" :age 22 :id 1}]
         (db/eav->map [[[1742966319 1843675177 1] "Joe"]
                       [[1742966319 -604307804 1] 22]]
                      {1742966319 :customer
                       1843675177 :name
                       -604307804 :age}))))

(deftest map->eav
  (is (= [{[-1059806875 1843675177 1] "Joe"}
          {-1059806875 :person
           1843675177 :name}]
         (db/map->eav #:person {:id 1 :name "Joe"}))))

(deftest maps->db
  (is (= {[-1059806875 1843675177 22] "Joe"
          [-1059806875 1843675177 33] "Jim"
          [] {-1059806875 :person
              1843675177 :name}}
         (db/maps->db #:person {:name "Joe" :id 22}
                      #:person {:name "Jim" :id 33}))))

(deftest find-attr
  (let [db (db/maps->db #:a {:id -1 :b "b1"}
                        #:a {:id 10 :b "b2"})]
    (is (= [[[-2123407586 1482224470 -1] "b1"]
            [[-2123407586 1482224470 10] "b2"]]
           (db/find-attr db :a :b nil)))
    (is (= [[[-2123407586 1482224470 10] "b2"]]
           (db/find-attr db :a :b [10])))))

(deftest find-entities
  (let [db (db/maps->db #:person {:age 22 :id 22}
                        #:person {:age 33 :id 33})]
    (is (= [33]
           (db/find-entities db [[-1059806875 -604307804 33]])))
    (is (= [22 33]
           (db/find-entities db [[-1059806875 -604307804 pos?]])))))

(def test-db (db/maps->db
              #:person {:id 1 :name "Sam" :age 23 :identity 42 :on? true :friend [:person/id 2]}
              #:person {:id 2 :name "Jim" :age 88 :friend [:person/id 1]}
              #:company {:id 1 :name "Co#1" :team [[:person/id 1] [:person/id 2]]}
              #:company {:id 2 :name "Co#2" :site "http://example.com"}))

(deftest query
  (testing "One prop"
    (is (= [#:person{:name "Sam" :id 1}
            #:person{:name "Jim" :id 2}]
           (db/query test-db [:person/name :person/id]))))
  (testing "Many props"
    (is (= [#:person{:name "Sam" :age 23 :id 1}
            #:person{:name "Jim" :age 88 :id 2}]
           (db/query test-db [:person/name :person/age]))))
  (testing "Missing props"
    (is (= [#:person{:name "Sam" :identity 42 :id 1}
            #:person{:name "Jim" :id 2}]
           (db/query test-db [:person/name :person/identity]))))
  (testing "Missing all props"
    (is (= []
           (db/query test-db [:person/a :person/b]))))
  (testing "Filter one prop"
    (is (= [#:person{:id 1 :name "Sam"}]
           (db/query test-db [{:person/on? true} :person/name]))))
  (testing "Filter all out"
    (is (= []
           (db/query test-db [{:person/on? nil} :person/name]))))
  (testing "Filter function"
    (is (= [#:person {:id 2 :name "Jim"}]
           (db/query test-db [{:person/age even?} :person/name]))))
  (testing "Filter many props"
    (is (= [#:person{:id 1 :age 23}]
           (db/query test-db [{:person/on? true :person/name "Sam"} :person/age]))))
  (testing "Filter many props + additional prop"
    (is (= [#:person{:id 1 :name "Sam"}]
           (db/query test-db [{:person/on? true} :person/name]))))
  (testing "Filter many props + additional prop"
    (is (= []
           (db/query test-db [{:person/name "Jane"}]))))
  (testing "Join"
    (is (= [{:person/id 1 :person/name "Sam" :person/friend {:person/id 2 :person/name "Jim"}}
            {:person/id 2 :person/name "Jim" :person/friend {:person/id 1 :person/name "Sam"}}]
           (db/query test-db [:person/name {:person/friend [:person/name]}])))))

(deftest save
  (testing "No new data"
    (is (= [test-db []]
           (db/save test-db [[[:person/age 1] 23]]))))
  (testing "New data"
    (let [prop [[:person/height 1] 177]]
      (is (= [(update (assoc test-db [(hash :person) (hash :height) 1] 177)
                      []
                      merge {(hash :height) :height}) [prop]]
             (db/save test-db [prop])))))
  (testing "Changed data"
    (let [prop [[:person/age 1] 100]]
      (is (= [(assoc test-db [(hash :person) (hash :age) 1] (second prop)) [prop]]
             (db/save test-db [prop])))))
  (testing "Remove existing data"
    (let [prop [[:person/age 1] nil]]
      (is (= [(dissoc test-db [(hash :person) (hash :age) 1]) [prop]]
             (db/save test-db [prop])))))
  (testing "Remove missing data"
    (let [prop [[:person/height 1] nil]]
      (is (= [test-db []]
             (db/save test-db [prop])))))
  (testing "Add/change/remove"
    (let [prop-add [[:person/height 2] 23]
          prop-change [[:person/name 1] "Yo"]
          prop-remove [[:person/age 2] nil]]
      (is (= [(-> test-db
                  (assoc [(hash :person) (hash :height) 2] (second prop-add))
                  (assoc [(hash :person) (hash :name) 1] (second prop-change))
                  (dissoc [(hash :person) (hash :age) 2])
                  (update [] merge {(hash :height) :height}))
              [prop-add prop-change prop-remove]]
             (db/save test-db [prop-add prop-change prop-remove]))))))

(deftest drop-entity
  (is (= (reduce (fn[acc [k v]]
                   (if (and (= (hash :person) (first k))
                            (= (last k) 1))
                     acc
                     (assoc acc k v)))
                 {} test-db)
         (db/drop-entity test-db :person 1))))
