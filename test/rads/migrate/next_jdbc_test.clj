(ns rads.migrate.next-jdbc-test
  (:refer-clojure :exclude [alter])
  (:require [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc]
            [rads.migrate :as migrate]
            [rads.migrate.next-jdbc :as migrate-next-jdbc]))

(defn- with-test-ds [f]
  (with-open [ds (jdbc/get-connection "jdbc:sqlite::memory:")]
    (f ds)))

(def migrations
  [{:id :seed
    :migrate (fn [{:keys [::print-fn]}] (print-fn "Seeding"))
    :rollback (fn [{:keys [::print-fn]}] (print-fn "Rolling back seed"))}

   {:id :alter
    :migrate (fn [{:keys [::print-fn]}] (print-fn "Altering"))
    :rollback (fn [{:keys [::print-fn]}] (print-fn "Rolling back alter"))}])

(deftest migrate-test
  (with-test-ds
    (fn [ds]
      (let [storage (migrate-next-jdbc/storage
                      {:ds ds
                       :events-table :migration-events})
            config {:migrations migrations
                    :storage storage
                    ::print-fn println}]
        (migrate/migrate! config)
        (is (= {:event-type :migrate
                :payload {:from-id :seed :to-id :alter}}
               (-> (migrate/get-last-event config)
                   (select-keys [:event-type :payload]))))
        (migrate/rollback! config)
        (is (= {:event-type :rollback
                :payload {:from-id :alter :to-id :seed}}
               (-> (migrate/get-last-event config)
                   (select-keys [:event-type :payload]))))))))
