# ACID
We'll go through each task sequentially, and I'll describe the solution strategy and how I achieved it.

It's also worth noting that we use `AcidTestDB` to get a connection to our database, since the main `Database` class returns a **HikariCP**-pooled connection, which would make the tests misleading.

## Task 1 — Consistency

To break consistency in our model, we use the strategy of creating an **orphan booking**: we set a parking slot to `booked` but never create a session for it. In this case consistency is broken, and we get:

```
[NO TRANSACTION]   slot.booked=true   activeSessions=0  -> 'booked <=> active session' violated
[WITH TRANSACTION] slot.booked=false  activeSessions=0  -> 'booked <=> active session' holds
```

In the non-transactional case, the slot is marked as booked but no session was created for that booking — **consistency is violated**.

## Task 2 — Isolation Levels

Here the most trivial example works best: under the default **Read Committed** level, two candidates in a race condition trying to book the same spot will result in a **double booking**. To avoid this, the best option is **Serializable** isolation.

To test this, we create an executor pool with 2 threads and use a `CountDownLatch` to force the race condition, so both threads start at exactly the same time.

## Task 3 — Indexing

For both this task and the 4th, population is done via **PL/pgSQL** scripts: first creating 1,000 slots, 100,000 number plates, and 1,000,000 parking-session records.

We then run a basic search by `slot_id`, add an index on `slot_id`, and run the same search again. The output is:

```
--- WITHOUT index ---
Planning Time: 0.087 ms
Execution Time: 20.116 ms
```

```
--- WITH index ---
Planning Time: 0.089 ms
Execution Time: 1.375 ms
```

The planning time is essentially unchanged, but the execution time differs by **~14.63×** (20.116 / 1.375) — the without-index case is over 14× slower.

## Task 4 — More Indexing

The test case here is finding an active slot of an exact type, so the compound index is **`(number_plate, active)`**. We play with the compound index by feeding it different keys: first a full key, then a prefix, then a suffix.

```
--- whole key ---
Planning Time: 0.101 ms
Execution Time: 0.026 ms
```

```
--- prefix ---
Planning Time: 0.019 ms
Execution Time: 0.035 ms
```

```
--- suffix ---
Planning Time: 0.014 ms
Execution Time: 51.966 ms
```

The whole-key case is obviously super fast thanks to the index, and so is the prefix. The interesting one was the **suffix**, with its huge execution time. The answer lies in how sorting with indexes works: a B-tree compound index is ordered **left to right**, so we cannot skip the leading column and jump to a trailing part of the key.

A query on `number_plate` (the leading column) — whole key or prefix — can seek directly into the index. But a query on `active` alone scatters across every `number_plate` value, so the planner can only fall back to a **full index scan** (never an efficient seek), which is why the suffix case is ~1500× slower.
 