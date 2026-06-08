### running
Create a `.env` file in the same directory as the manifest (`compose.yaml`), with
these **UPPERCASE** variables — or just `cp .env.example .env` and fill them in:

```dotenv
POSTGRES_DB=parking
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
```

> Use `KEY=value` with **no spaces** around `=`. `.env` is local-only

then
```bash
podman compose up -d
```

- Compose reads the `${POSTGRES_*}` values from `.env`.
- On the **first** start, the SQL in `database/` is auto-run and the schema is
  created.
- Data lives in the named volume `postgres_data` (mounted at
  `/var/lib/postgresql/data`), so it **survives the container shutting down or
  restarting**.

  
### for testing

```bash
mvn test                             # integration tests 
USE_IN_MEMORY=true mvn test          # integration tests for in-memory
```


## notes 

