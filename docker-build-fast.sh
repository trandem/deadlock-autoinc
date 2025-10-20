#!/bin/bash

# Fast Docker Build Script for Car Park API
# Uses BuildKit for maximum speed with cache mounts

set -e

echo "🚀 Fast Docker Build with BuildKit"
echo "=================================="

# Check if BuildKit is available
if ! docker buildx version > /dev/null 2>&1; then
    echo "⚠️  Docker BuildKit not available, using standard build"
    docker build -t carpark-api .
else
    echo "✅ Using BuildKit with cache mounts for fastest build"

    # Use BuildKit with cache mounts
    DOCKER_BUILDKIT=1 docker build \
        --file Dockerfile.fast \
        --tag carpark-api:latest \
        --tag carpark-api:java21 \
        --progress=plain \
        .
fi

echo ""
echo "✅ Build complete!"
echo "Run with: docker-compose up"
echo "Or: docker run -p 8080:8080 carpark-api"