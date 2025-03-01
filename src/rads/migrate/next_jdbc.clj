(ns rads.migrate.next-jdbc
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [rads.migrate.storage :as storage])
  (:import (java.time Instant)))

(defn- table-name [table]
  (str/replace (name table) "-" "_"))

(defrecord Storage [ds events-table]
  storage/Storage
  (with-transaction [_ f]
    (jdbc/with-transaction+options [tx ds]
      (f tx)))

  (init [_]
    (jdbc/execute-one!
      ds
      [(format
         "CREATE TABLE IF NOT EXISTS %s (
            id text primary key,
            timestamp text not null,
            event_type text not null,
            payload text not null
          )"
         (table-name events-table))])
    (jdbc/execute-one!
      ds
      [(apply format
              "CREATE INDEX IF NOT EXISTS idx_%s_timestamp ON %s(timestamp)"
              (repeat 2 (table-name events-table)))]))

  (get-last-event [_ tx]
    (->> (sql/find-by-keys tx
                           (table-name events-table)
                           :all
                           {:order-by [[:timestamp :desc]]
                            :limit 1})
         (map (fn [row]
                (-> row
                    (update :timestamp #(Instant/parse %))
                    (update :event-type edn/read-string)
                    (update :payload edn/read-string))))
         first))

  (append-event [_ tx event]
    (let [row (update event :payload pr-str)]
      (sql/insert! tx (table-name events-table) row))))


(defn storage [opts]
  (let [ds' (jdbc/with-options (:ds opts) jdbc/unqualified-snake-kebab-opts)
        defaults {:events-table :migration-events}
        opts' (merge defaults
                     (select-keys opts [:events-table])
                     {:ds ds'})]
    (map->Storage opts')))
