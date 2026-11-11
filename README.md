# Payment Engine

A P2P payment engine in plain Java 21 (no frameworks), built to practice concurrency:
money is never lost or duplicated under parallel load.

Work in progress. The plan and the current stage are in [ROADMAP.md](ROADMAP.md).

## Build and test

```
./mvnw -B verify
```

## Run the load simulator

```
./mvnw -q -B package
java -jar engine/target/engine.jar --strategy=naive
```

`naive` has no locking and loses money on purpose. Other strategies (`global`, `ordered`, `trylock`,
`auditable`) become available as the roadmap stages are completed.

Options: `--threads`, `--transfers`, `--accounts`, `--balance`, `--max-amount`.
