# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Commands

### Testing
- Run all tests with cognitect test runner: `bb test`

### Development
- Start nREPL: `bb dev`

## Project Architecture

This is a minimal Clojure migration library with zero runtime dependencies. The architecture follows a pluggable storage backend pattern:

### Core Components

1. **Main API (`rads.migrate`)**: Provides `migrate!` and `rollback!` functions that orchestrate migration execution
2. **Storage Protocol (`rads.migrate.storage`)**: Defines the `Storage` protocol for pluggable backends
3. **Next.jdbc Implementation (`rads.migrate.next-jdbc`)**: Default storage backend using next.jdbc

### Key Concepts

- **Migrations**: Sequences of maps with `:id`, `:migrate`, and optional `:rollback` functions
- **Event Sourcing**: All migration events are logged to storage with timestamps and payloads
- **Transactional**: Operations run within database transactions for consistency
- **Backend Agnostic**: Core logic is separated from storage implementation

### Configuration Pattern

The library uses a config map pattern with `:storage` and `:migrations` keys. Storage backends implement transaction handling, event persistence, and initialization.

### File Organization

- `/src/rads/migrate.clj`: Core migration engine
- `/src/rads/migrate/storage.clj`: Storage protocol definition  
- `/src/rads/migrate/next_jdbc.clj`: Database storage implementation
- `/test/`: Test files following namespace structure
