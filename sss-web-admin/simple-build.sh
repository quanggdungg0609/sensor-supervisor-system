#!/bin/bash

# Simple single-platform build script for sss-web-admin
# This script builds for AMD64 only and pushes to Docker registry
# Usage: ./simple-build.sh --registry <registry> --username <user> --password <pass>

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
    -h|--help)
      echo "Usage: $0 --registry <registry> --username <user> --password <pass>"
      echo "Example: $0 --registry 192.168.152.117:5000 --username dev --password licornerose25"
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

echo -e "${GREEN}Building ${APP_NAME} version ${VERSION} (AMD64 only)${NC}"
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

# Build image for AMD64
echo -e "${YELLOW}Building Docker image (AMD64)...${NC}"
docker build --platform linux/amd64 -t "${IMAGE_NAME}:${VERSION}" -t "${IMAGE_NAME}:latest" .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully built image${NC}"
else
    echo -e "${RED}Failed to build image${NC}"
    exit 1
fi

# Push images
echo -e "${YELLOW}Pushing images to registry...${NC}"
docker push "${IMAGE_NAME}:${VERSION}"
docker push "${IMAGE_NAME}:latest"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully pushed images${NC}"
else
    echo -e "${RED}Failed to push images${NC}"
    exit 1
fi

# Logout from registry
echo -e "${YELLOW}Logging out from registry...${NC}"
docker logout "$REGISTRY"

echo -e "${GREEN}Build and push completed successfully!${NC}"
echo -e "${GREEN}Image: ${IMAGE_NAME}:${VERSION}${NC}"
echo -e "${GREEN}Platform: linux/amd64${NC}"