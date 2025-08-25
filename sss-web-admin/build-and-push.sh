#!/bin/bash

# Build and push script for sss-web-admin
# This script builds the Next.js application for multiple platforms and pushes it to Docker registry
# Usage: ./build-and-push.sh --registry <registry> --username <user> --password <pass>

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
    --platforms)
      PLATFORMS="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 --registry <registry> --username <user> --password <pass> [--platforms <platforms>]"
      echo "Example: $0 --registry 192.168.152.117:5000 --username dev --password licornerose25"
      echo "Platforms: $PLATFORMS (default)"
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
echo -e "${BLUE}Platforms: ${PLATFORMS}${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Check if buildx is available and setup insecure registry builder
echo -e "${YELLOW}Setting up Docker buildx for insecure registry...${NC}"

# Create or use a buildx builder that supports insecure registries
BUILDER_NAME="insecure-registry-builder"

# Remove existing builder if it exists to recreate with proper config
echo -e "${YELLOW}Removing existing buildx builder if present...${NC}"
docker buildx rm "$BUILDER_NAME" > /dev/null 2>&1 || true

# Create buildx configuration file
echo -e "${YELLOW}Creating buildx configuration...${NC}"
mkdir -p ~/.docker/buildx
cat > ~/.docker/buildx/buildkitd.toml <<EOF
[registry."$REGISTRY"]
  http = true
  insecure = true
EOF

# Create new builder with proper configuration
echo -e "${YELLOW}Creating new buildx builder for insecure registry...${NC}"
docker buildx create --name "$BUILDER_NAME" \
    --driver docker-container \
    --driver-opt network=host \
    --config ~/.docker/buildx/buildkitd.toml

# Use the builder
docker buildx use "$BUILDER_NAME"

# Bootstrap the builder
echo -e "${YELLOW}Bootstrapping buildx builder...${NC}"
docker buildx inspect --bootstrap

# Login to registry
echo -e "${YELLOW}Logging into registry...${NC}"
echo "$PASSWORD" | docker login "$REGISTRY" --username "$USERNAME" --password-stdin

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully logged into registry${NC}"
else
    echo -e "${RED}Failed to login to registry${NC}"
    exit 1
fi

# Build and push multi-platform image
echo -e "${YELLOW}Building and pushing multi-platform Docker image...${NC}"
docker buildx build \
  --platform "$PLATFORMS" \
  --tag "${IMAGE_NAME}:${VERSION}" \
  --tag "${IMAGE_NAME}:latest" \
  --push \
  .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully built and pushed multi-platform image${NC}"
    echo -e "${GREEN}Image: ${IMAGE_NAME}:${VERSION}${NC}"
    echo -e "${GREEN}Platforms: ${PLATFORMS}${NC}"
else
    echo -e "${RED}Failed to build and push image${NC}"
    exit 1
fi

# Logout from registry
echo -e "${YELLOW}Logging out from registry...${NC}"
docker logout "$REGISTRY"

# Cleanup: Remove the temporary builder and config
echo -e "${YELLOW}Cleaning up buildx builder and config...${NC}"
docker buildx use default > /dev/null 2>&1 || true
docker buildx rm "$BUILDER_NAME" > /dev/null 2>&1 || true
rm -f ~/.docker/buildx/buildkitd.toml > /dev/null 2>&1 || true

echo -e "${GREEN}Build and push completed successfully!${NC}"
echo -e "${GREEN}Image: ${IMAGE_NAME}:${VERSION}${NC}"
echo -e "${GREEN}Platforms: ${PLATFORMS}${NC}"