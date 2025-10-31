# Optimized Password Cracker

## Quick Start

**Optimal command for maximum performance:**

```bash
java -Xms512m -Xmx1g -XX:+UseParallelGC \
     -jar run.jar datasets/large/in.txt datasets/large/dictionary.txt out.txt
```

**Expected performance**: Depends on dataset size and slowdown factor (NUM_ITERATIONS)

> **Note**: The code includes an intentional slowdown mechanism (1M iterations per hash) to simulate realistic workload and prevent unrealistically fast benchmarking times.

## What's Included

This JAR contains only the essential optimized components:

### Core Classes
- `PasswordCracker` - Main orchestrator
- `PasswordCrackingEngine` - Optimized O(N+M) algorithm with work-stealing thread pool
- `HashUtil` - High-performance SHA-256 with ThreadLocal and lookup tables
- `HashLoaderService` - Optimized user loading with BufferedReader
- `DictionaryLoaderService` - Optimized dictionary loading
- `OutputWriterService` - Result output
- `StatusReporterService` - Progress tracking

### Data Models
- `User` - Username + hash record
- `CrackResult` - Cracked password result
- `CrackingStatistics` - Thread-safe statistics with AtomicLong

## Key Optimizations

✅ **Algorithm**: O(N+M) hash lookup instead of O(N×M) nested loops
✅ **Hashing**: ThreadLocal MessageDigest + hex lookup table + pre-computed waste bytes
✅ **Threading**: Work-stealing pool with optimal chunking (4 threads for 4-core VM)
✅ **I/O**: BufferedReader with pre-sized collections
✅ **Collections**: Pre-sized HashMap and ArrayList
✅ **Memory**: Pre-computed 1M waste byte arrays (~50MB) eliminates string allocations
✅ **JVM Flags**: Optimized for multi-threaded throughput

## JVM Flags Explained

The recommended flags are optimized for the 4-core VM with slowdown mechanism:

```bash
-Xms512m -Xmx1g -XX:+UseParallelGC
```

### Flag Breakdown:

- **`-Xms512m`**: Initial heap size (512MB)
  - Prevents initial heap expansions during warmup
  - Sufficient for ~50MB pre-computed waste bytes + working memory

- **`-Xmx1g`**: Maximum heap size (1GB)
  - Provides headroom for large datasets
  - Prevents OutOfMemoryError with concurrent processing

- **`-XX:+UseParallelGC`**: Parallel Garbage Collector
  - Best for multi-threaded throughput workloads
  - Utilizes all 4 CPU cores for GC
  - Superior to SerialGC for concurrent hash computation

### Why These Flags?

1. **Throughput > Startup Time**: With slowdown, each hash takes ~30ms, so JIT warmup cost is negligible
2. **Multi-threaded Workload**: ParallelGC leverages all cores during GC pauses
3. **Memory-Intensive**: Pre-computed waste bytes require stable heap allocation

## Performance Characteristics

- **Small dataset** (100 users, 70 passwords): ~2.2 seconds
- **Large dataset** (10,000 users, 7,976 passwords): ~4-5 minutes
- **Per-hash time**: ~30-32ms (dominated by 1M iteration slowdown)
- **Thread count**: 4 (hardcoded for 4-core VM, configurable via `PasswordCracker` constructor)

## Advanced Usage

**Testing without slowdown (modify NUM_ITERATIONS in HashUtil.java):**
```java
private static final int NUM_ITERATIONS = 0; // Disable slowdown for testing
```

**Memory profiling:**
```bash
java -Xms512m -Xmx1g -XX:+UseParallelGC -XX:+PrintGCDetails \
     -jar run.jar datasets/large/in.txt datasets/large/dictionary.txt out.txt
```

**Performance tuning for different VM configurations:**
- 2 cores: Use `-Dnum.threads=2` or modify `PasswordCracker` constructor
- 8+ cores: Consider increasing thread count and heap size proportionally

## System Requirements

- Java 21
- Linux VM with 4 CPU cores
- 512MB RAM minimum
- CPU with SHA-NI extensions (optional but recommended)

## Input/Output Format

**Input file** (CSV):
```
username,sha256_hash
```

**Dictionary file** (one password per line):
```
password1
password2
...
```

**Output file** (CSV):
```
username,sha256_hash,plain_password
```
