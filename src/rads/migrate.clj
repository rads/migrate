(ns rads.migrate
  (:require [rads.migrate.storage :as storage])
  (:import (java.time Instant)))

(defn get-last-event [config]
  (let [{:keys [storage]} config]
    (storage/get-last-event storage (:ds storage))))

(defn- find-prev-migration [migrations current-migration-id]
  (second (drop-while #(not= current-migration-id (:id %)) (rseq migrations))))

(defn- find-current-migration [migrations current-migration-id]
  (first (drop-while #(not= current-migration-id (:id %)) migrations)))

(defn- find-remaining-migrations [migrations current-migration-id]
  (if current-migration-id
    (rest (drop-while #(not= current-migration-id (:id %)) migrations))
    migrations))

(defn- migrate-event [current-migration-id next-migration]
  {:id (random-uuid)
   :timestamp (Instant/now)
   :event-type :migrate
   :payload {:from-id current-migration-id
             :to-id (:id next-migration)}})

(defn migrate!
  "Run migrations."
  [config]
  (let [{:keys [storage migrations]} config]
   (storage/init storage)
   (storage/with-transaction
     storage
     (fn [tx]
       (let [current-migration-id (-> (storage/get-last-event storage tx)
                                      :payload :to-id)
             remaining (find-remaining-migrations migrations current-migration-id)]
         (when (seq remaining)
           (loop [[migration & xs] remaining
                  cur-id current-migration-id]
             (let [event (migrate-event cur-id migration)]
               ((:migrate migration) (assoc config :tx tx))
               (storage/append-event storage tx event)
               (when (seq xs)
                 (recur xs (:id migration)))))))))))

(defn- rollback-event [current-migration-id prev-migration]
  {:id (random-uuid)
   :timestamp (Instant/now)
   :event-type :rollback
   :payload {:from-id current-migration-id
             :to-id (:id prev-migration)}})

(defn rollback!
  "Rollback to the previous migration."
  [config]
  (let [{:keys [storage migrations]} config]
   (storage/init storage)
   (storage/with-transaction
     storage
     (fn [tx]
       (let [current-migration-id (-> (storage/get-last-event storage tx)
                                      :payload :to-id)
             current-migration (find-current-migration migrations
                                                       current-migration-id)
             prev-migration (find-prev-migration migrations
                                                 current-migration-id)
             event (rollback-event current-migration-id prev-migration)]
         ((:rollback current-migration) (assoc config :tx tx))
         (storage/append-event storage tx event))))))
