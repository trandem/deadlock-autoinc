# AI Tool Usage Documentation

This document details how Claude Code AI was used to complete this coding exercise, including the prompts, techniques, and workflow.

## Generate README
Help me generate the documentation for `README.md`. Emphasize:
- Key performance improvements: multithreading, upsert (insert on duplicate key update), and sharding when processing availability.
- Remove database-level locks: the application generates IDs, and there are no foreign keys (the application handles this).
- Show a warning when a park number cannot be found.
- Use `start.sh` for easy project startup.
- Use green threads because the application is I/O intensive.
- Add two columns, `total_available_lots` and `total_lots`, to simplify queries for the nearest location (avoiding join queries to improve performance).
- Include project trade-offs and technical stack decisions when designing the application (denormalization, no foreign keys, green threads, sharding, upsert, multithreading).

## Generate Dockerfile
Help me generate a `Dockerfile` for this project and a `docker-compose.yml` to run the application with a MySQL database.

## Generate start.sh
Help me generate `start.sh` for easy project startup. Include commands to create the database, run migrations, and start the application.

## Generate test
