# Glacial

A lightweight Redis-compatible server written in Java 21+, using zero runtime dependencies.

## Building

```bash
./gradlew build
```

## Running the Server

```bash
# Default: threaded mode on port 6379
./gradlew run

# Custom port and mode
java -cp build/classes/java/main org.iceberg.Main --port 6380 --mode threaded

# Async NIO mode
java -cp build/classes/java/main org.iceberg.Main --port 6380 --mode async

# Custom persistence directory
java -cp build/classes/java/main org.iceberg.Main --port 6380 --dir /var/data/glacial
```

### Server Options

| Flag | Short | Default | Description |
|------|-------|---------|-------------|
| `--port` | `-p` | `6379` | TCP port to listen on |
| `--mode` | `-m` | `threaded` | `threaded` (virtual threads) or `async` (NIO2) |
| `--dir` | `-d` | `.` | Directory for `dump.rdb` persistence file |

## Using the CLI

The CLI is located at `cli/glacial-cli.py`. Run it from the `cli/` directory or use an absolute path.

```bash
python3 cli/glacial-cli.py -p 6379 <command> [args...]

# Examples
python3 cli/glacial-cli.py -p 6379 SET mykey "hello world"
python3 cli/glacial-cli.py -p 6379 GET mykey
```

### CLI Options

| Flag | Short | Default | Description |
|------|-------|---------|-------------|
| `--host` | `-H` | `localhost` | Server host |
| `--port` | `-p` | `6379` | Server port |

## Commands

### PING

Test server connectivity.

```
> PING
PONG
```

### ECHO

Return the given string.

```
> ECHO hello
hello
```

### SET key value

Set a string key to a string value. Returns `OK`.

```
> SET name John
OK
> GET name
John
```

### GET key

Get the string value of a key. Returns `(nil)` if the key does not exist.

```
> GET name
John
> GET missing
(nil)
```

### EXISTS key [key ...]

Count how many of the given keys exist. Returns an integer.

```
> SET a 1
OK
> EXISTS a
1
> EXISTS a b
1
> EXISTS nokey
0
```

### DEL key [key ...]

Delete one or more keys. Returns the number of keys removed.

```
> SET a 1
OK
> SET b 2
OK
> DEL a b
2
> EXISTS a
0
```

### INCR key

Increment the integer value of a key by one. If the key does not exist, it is set to `0` before incrementing. Returns an error if the value is not an integer.

```
> INCR counter
1
> INCR counter
2
> INCR counter
3
> SET str abc
OK
> INCR str
(error) ERR value is not an integer or out of range
```

### DECR key

Decrement the integer value of a key by one. If the key does not exist, it is set to `0` before decrementing. Returns an error if the value is not an integer.

```
> DECR counter
-1
> DECR counter
-2
```

### LPUSH key value [value ...]

Insert one or more values at the head of a list. Returns the list length after the push. Creates the list if it does not exist.

```
> LPUSH mylist a
1
> LPUSH mylist b
2
> LPUSH mylist c
3
> LRANGE mylist 0 -1
3 items
[0]   c
[1]   b
[2]   a
```

### RPUSH key value [value ...]

Insert one or more values at the tail of a list. Returns the list length after the push. Creates the list if it does not exist.

```
> RPUSH mylist x
4
> RPUSH mylist y
5
> LRANGE mylist 0 -1
5 items
[0]   c
[1]   b
[2]   a
[3]   x
[4]   y
```

### LRANGE key start stop

Return a range of elements from a list. Indices are zero-based. Negative indices count from the end (`-1` is the last element). Use `0 -1` to get all elements.

```
> LRANGE mylist 0 2
3 items
[0]   c
[1]   b
[2]   a
> LRANGE mylist -2 -1
2 items
[0]   x
[1]   y
```

### SAVE

Synchronously save the database state to disk as `dump.rdb`. The server loads this file automatically on startup.

```
> SAVE
OK
```

## Persistence

Glacial persists data to `dump.rdb` (configurable via `--dir`). On startup, the server loads the dump file if it exists. The `SAVE` command writes the current state to disk synchronously.

```bash
# Server saves to /var/data/glacial/dump.rdb
java -cp build/classes/java/main org.iceberg.Main --dir /var/data/glacial

# Run SAVE from CLI to persist
python3 cli/glacial-cli.py -p 6379 SAVE
```

## Benchmarking

```bash
# Threaded mode benchmark
./gradlew benchmark

# Async mode benchmark
./gradlew benchmarkAsync

# Custom benchmark options
java -cp build/classes/java/main org.iceberg.benchmark.Benchmark \
  -m threaded -n 50000 -c 100 -q
```

| Flag | Default | Description |
|------|---------|-------------|
| `-m` | `threaded` | Server mode (`threaded` or `async`) |
| `-n` | `10000` | Total number of requests |
| `-c` | `50` | Number of concurrent clients |
| `-q` | `false` | Quiet mode (suppress output) |
| `-p` | `6379` | Server port |

## Running Tests

```bash
./gradlew test
```

## Architecture

- **Zero runtime dependencies** -- pure Java 21+ with virtual threads and sealed interfaces
- **Two server backends** -- threaded (virtual threads + `ServerSocket`) and async (NIO2 + `CompletionHandler`)
- **RESP protocol** -- hand-rolled parser supporting all 5 RESP types
- **Thread-safe storage** -- `ConcurrentHashMap` with `CopyOnWriteArrayList` for lists
