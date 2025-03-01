# rads.migrate

[![Clojars Project](https://img.shields.io/clojars/v/io.github.rads/migrate.svg)](https://clojars.org/io.github.rads/migrate)

A minimal migration library in pure Clojure.

## Features

- Supports any storage backend with defaults for next.jdbc.
- All migration events are logged.
- Migrations are sequences of maps.
- Each migration map has the following keys:
    - `:id` (required) - A unique ID.
    - `:migrate` (required) - A function to run the migration.
    - `:rollback` (optional) - A function to rollback the migration.

## Usage

```clojure
(require '[next.jdbc :as jdbc]
         '[rads.migrate :as migrate]
         '[rads.migrate.next-jdbc :as migrate-next-jdbc])

(defn migrations [{:keys [print-fn]}]
  [{:id :seed
    :migrate (fn [_] (print-fn "Seeding"))
    :rollback (fn [_] (print-fn "Rolling back seed"))}

   {:id :alter
    :migrate (fn [_] (print-fn "Altering"))
    :rollback (fn [_] (print-fn "Rolling back alter"))}])

(defn migration-config [opts]
 {:storage (migrate-next-jdbc/storage opts)
  :migrations (migrations opts)})

(comment
  (def config
    (migration-config
      {:ds (jdbc/get-datasource "jdbc:sqlite:app.db")
       :print-fn println}))

  ;; Migrate to the latest migration
  (migrate/migrate! config)

  ;; Rollback to the previous migration
  (migrate/rollback! config)
  
  ;; Get last migration event
  (migrate/get-last-event config))
```

## License

Copyright © 2025 Radford Smith

rads.migrate is distributed under the [MIT License](LICENSE).
