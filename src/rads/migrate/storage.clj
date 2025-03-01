(ns rads.migrate.storage)

(defprotocol Storage
  (with-transaction [storage f])
  (init [storage])
  (append-event [storage tx event])
  (get-last-event [storage tx]))
