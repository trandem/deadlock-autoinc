# Quick Start - Fast Docker Builds

Choose your speed! ⚡

## 🚀 Fastest Build (Recommended)

Using BuildKit with cache mounts:

```bash
# One command - fastest build
./docker-build-fast.sh

# Then start all services
docker-compose up
```

**First build**: ~1-2 minutes
**Rebuild**: ~10-15 seconds

---

## ⚡ Fast Build (Standard)

Using optimized Dockerfile with Alpine:

```bash
# Build and start
docker-compose up --build
```

**First build**: ~2-3 minutes
**Rebuild**: ~30 seconds

---

## 🔥 Super Fast Build (All Optimizations)

Use the optimized docker-compose:

```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Build and start with optimizations
docker-compose -f docker-compose.fast.yml up --build
```

**First build**: ~1 minute
**Rebuild**: ~10 seconds

---

## Common Commands

### Build only (no start):
```bash
docker-compose build
```

### Force rebuild (clear cache):
```bash
docker-compose build --no-cache
```

### Stop and remove everything:
```bash
docker-compose down -v
```

### View logs:
```bash
docker-compose logs -f app
```

### Restart just the app:
```bash
docker-compose restart app
```

---

## Performance Comparison

| Method | First Build | Rebuild | Image Size |
|--------|-------------|---------|------------|
| Old Dockerfile | 8-10 min | 8-10 min | 500MB |
| **Standard (Dockerfile)** | **2-3 min** | **30 sec** | **200MB** |
| **Fastest (Dockerfile.fast)** | **1-2 min** | **10 sec** | **200MB** |

---

## Quick Test

After starting:

```bash
# Test API is running
curl http://localhost:8080/actuator/health

# Test virtual threads
curl http://localhost:8080/api/demo/virtual-threads/info

# Test car park API
curl "http://localhost:8080/api/carparks/nearest?latitude=1.3521&longitude=103.8198&page=1&per_page=10"
```

---

## Troubleshooting

### Build taking too long?
1. Enable BuildKit: `export DOCKER_BUILDKIT=1`
2. Use fast script: `./docker-build-fast.sh`
3. Check if using alpine base: `docker images | grep carpark`

### Out of disk space?
```bash
# Clean old images
docker system prune -a

# Remove unused volumes
docker volume prune
```

### Dependencies not caching?
```bash
# Rebuild with verbose output
DOCKER_BUILDKIT=1 docker build --progress=plain -f Dockerfile.fast .
```

---

## Files Overview

| File | Purpose | Speed |
|------|---------|-------|
| `Dockerfile` | Standard optimized build | Fast ⚡ |
| `Dockerfile.fast` | BuildKit + cache mounts | Fastest 🚀 |
| `docker-compose.yml` | Standard compose | Good |
| `docker-compose.fast.yml` | Optimized compose | Best 🔥 |
| `docker-build-fast.sh` | Build script | Easy ✨ |

---

## One-Command Setup

Complete setup from scratch:

```bash
# Clone and start (if repo exists)
git clone <repo-url>
cd demo

# Fast build and start
export DOCKER_BUILDKIT=1
docker-compose -f docker-compose.fast.yml up --build

# Wait for health check...
# ✅ API ready at http://localhost:8080
```

---

**See `DOCKER_OPTIMIZATION.md` for detailed explanations!**