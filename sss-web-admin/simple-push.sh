#!/bin/bash

# Simple build and push script for Rancher Desktop
# Usage: ./simple-push.sh --registry <registry> --username <user> --password <pass>

set -e

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
      echo "Example: $0 --registry 192.168.152.117:5000 --username dev --password yourpass"
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

# Build for AMD64 first (since it works)
echo -e "${YELLOW}Building AMD64 image...${NC}"
docker build --platform linux/amd64 -t "${IMAGE_NAME}:${VERSION}-amd64" .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully built AMD64 image${NC}"
else
    echo -e "${RED}Failed to build AMD64 image${NC}"
    exit 1
fi

# Push AMD64 image
echo -e "${YELLOW}Pushing AMD64 image...${NC}"
docker push "${IMAGE_NAME}:${VERSION}-amd64"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully pushed AMD64 image${NC}"
else
    echo -e "${RED}Failed to push AMD64 image${NC}"
    exit 1
fi

# Tag as latest
docker tag "${IMAGE_NAME}:${VERSION}-amd64" "${IMAGE_NAME}:latest"
docker push "${IMAGE_NAME}:latest"

# Logout from registry
echo -e "${YELLOW}Logging out from registry...${NC}"
docker logout "$REGISTRY"

echo -e "${GREEN}Build and push completed successfully!${NC}"
echo -e "${GREEN}AMD64 Image: ${IMAGE_NAME}:${VERSION}-amd64${NC}"
echo -e "${GREEN}Latest Image: ${IMAGE_NAME}:latest${NC}"