#!/usr/bin/env python3
"""Apply the schema and search indexes to an existing PostgreSQL database.

Both files run in one transaction; a failure rolls back the complete setup.
The target database must already exist. This is a fresh-database bootstrap;
use versioned Flyway migrations for existing databases.

Usage:
    Set DATABASE_URL to a PostgreSQL connection string.
    python apply_schema.py --dry-run
    python apply_schema.py

The setup role needs permission to create extensions and roles.
Dependency: pip install psycopg2-binary
"""
import argparse
import importlib
import os
import sys
from pathlib import Path

SCHEMA = Path(__file__).with_name("schema.sql")
INDEXES = Path(__file__).with_name("search_indexes.sql")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dsn", default=os.environ.get("DATABASE_URL"), help="padrão: $DATABASE_URL")
    ap.add_argument("--schema", type=Path, default=SCHEMA, help="arquivo SQL do schema")
    ap.add_argument("--indexes", type=Path, default=INDEXES, help="arquivo SQL dos índices")
    ap.add_argument("--dry-run", action="store_true", help="apenas mostra o SQL")
    args = ap.parse_args()

    if not args.schema.exists():
        sys.exit(f"Arquivo não encontrado: {args.schema}")
    if not args.indexes.exists():
        sys.exit(f"Arquivo não encontrado: {args.indexes}")
    schema_sql = args.schema.read_text(encoding="utf-8")
    indexes_sql = args.indexes.read_text(encoding="utf-8")

    if args.dry_run:
        print("-- Schema")
        print(schema_sql)
        print("-- Search indexes")
        print(indexes_sql)
        return
    if not args.dsn:
        sys.exit("Defina DATABASE_URL ou use --dsn.")

    try:
        psycopg2 = importlib.import_module("psycopg2")
    except ModuleNotFoundError:
        sys.exit("Instale psycopg2-binary para conectar ao PostgreSQL.")

    conn = psycopg2.connect(args.dsn)
    try:
        with conn:                      # commit se ok, rollback se exceção
            with conn.cursor() as cur:
                cur.execute(schema_sql)
                cur.execute(indexes_sql)
        print("Schema and search indexes applied successfully.")
    except psycopg2.Error as e:
        print(f"ERRO (nada foi aplicado): {(e.pgerror or str(e)).strip()}")
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()