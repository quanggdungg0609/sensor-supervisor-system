#!/bin/bash

# Robust build and push script for sss-web-admin
# This script tries buildx for multi-platform builds, with fallback to single-platform
# Usage: ./robust-build.sh --registry <registry> --username <user> --password <pass>

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REGISTRY=""
USERNAME=""
PASSWORD=""
PLATFORMS="linux/amd64,linux/arm64"
USE_BUILDX=true

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --registry)
      REGISTRY="$2"
      shift 2
      ;;
    --username)
      USERNAME="$2"
      shift 2
      ;;
    --password)
      PASSWORD="$2"
      shift 2
      ;;
    --single-platform)
      USE_BUILDX=false
      shift
      ;;
    -h|--help)
      echo "Usage: $0 --registry <registry> --username <user> --password <pass> [--single-platform]"
      echo "Example: $0 --registry 192.168.152.117:5000 --username dev --password licornerose25"
      echo "Options:"
      echo "  --single-platform    Force single-platform build (AMD64 only)"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option $1${NC}"
      exit 1
      ;;
  esac
done

# Validate required arguments
if [[ -z "$REGISTRY" || -z "$USERNAME" || -z "$PASSWORD" ]]; then
    echo -e "${RED}Error: Missing required arguments${NC}"
    echo "Usage: $0 --registry <registry> --username <user> --password <pass>"
    echo "Example: $0 --registry 192.168.152.117:5000 --username dev --password licornerose25"
    exit 1
fi

# Get version from package.json
VERSION=$(node -p "require('./package.json').version")
APP_NAME="sss-web-admin"
IMAGE_NAME="${REGISTRY}/quangdung/${APP_NAME}"

echo -e "${GREEN}Building ${APP_NAME} version ${VERSION}${NC}"
echo -e "${BLUE}Registry: ${REGISTRY}${NC}"
echo -e "${BLUE}Image: ${IMAGE_NAME}${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Login to registry
echo -e "${YELLOW}Logging into registry...${NC}"
echo "$PASSWORD" | docker login "$REGISTRY" --username "$USERNAME" --password-stdin

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully logged into registry${NC}"
else
    echo -e "${RED}Failed to login to registry${NC}"
    exit 1
fi

# Function to try buildx multi-platform build
try_buildx_build() {
    echo -e "${YELLOW}Attempting multi-platform build with buildx...${NC}"
    
    # Set Docker context to default
    docker context use default > /dev/null 2>&1 || true
    
    # Use a simple approach - just specify insecure registry via daemon
    echo -e "${YELLOW}Building with insecure registry support...${NC}"
    
    # Try to build with --allow-insecure-entitlement
    docker buildx build \
        --platform "$PLATFORMS" \
        --tag "${IMAGE_NAME}:${VERSION}" \
        --tag "${IMAGE_NAME}:latest" \
        --allow security.insecure \
        --push \
        . 2>/dev/null
    
    return $?
}

# Function to do single-platform build
do_single_platform_build() {
    echo -e "${YELLOW}Performing single-platform build (AMD64)...${NC}"
    
    # Build image for AMD64
    docker build --platform linux/amd64 -t "${IMAGE_NAME}:${VERSION}" -t "${IMAGE_NAME}:latest" .
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully built image${NC}"
    else
        echo -e "${RED}Failed to build image${NC}"
        return 1
    fi
    
    # Push images
    echo -e "${YELLOW}Pushing images to registry...${NC}"
    docker push "${IMAGE_NAME}:${VERSION}"
    docker push "${IMAGE_NAME}:latest"
    
    return $?
}

# Main build logic
BUILD_SUCCESS=false

if [ "$USE_BUILDX" = true ]; then
    echo -e "${BLUE}Trying multi-platform build with buildx...${NC}"
    if try_buildx_build; then
        echo -e "${GREEN}Multi-platform buildx build succeeded!${NC}"
        BUILD_SUCCESS=true
    else
        echo -e "${YELLOW}Multi-platform buildx build failed, falling back to single-platform...${NC}"
        if do_single_platform_build; then
            echo -e "${GREEN}Single-platform fallback build succeeded!${NC}"
            BUILD_SUCCESS=true
        fi
    fi
else
    echo -e "${BLUE}Using single-platform build as requested...${NC}"
    if do_single_platform_build; then
        echo -e "${GREEN}Single-platform build succeeded!${NC}"
        BUILD_SUCCESS=true
    fi
fi

# Check final result
if [ "$BUILD_SUCCESS" = false ]; then
    echo -e "${RED}All build attempts failed${NC}"
    exit 1
fi

# Logout from registry
echo -e "${YELLOW}Logging out from registry...${NC}"
docker logout "$REGISTRY"

echo -e "${GREEN}Build and push completed successfully!${NC}"
echo -e "${GREEN}Image: ${IMAGE_NAME}:${VERSION}${NC}"
if [ "$USE_BUILDX" = true ]; then
    echo -e "${GREEN}Platforms: Multi-platform attempted, check output above${NC}"
else
    echo -e "${GREEN}Platform: linux/amd64${NC}"
fi