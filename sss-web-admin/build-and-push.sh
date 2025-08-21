#!/bin/bash

# Build and push script for sss-web-admin
# This script builds the Next.js application and pushes it to Docker registry

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get version from package.json
VERSION=$(node -p "require('./package.json').version")
APP_NAME="sss-web-admin"
REGISTRY="your-registry.com"  # Replace with your actual registry
IMAGE_NAME="${REGISTRY}/${APP_NAME}"

echo -e "${GREEN}Building ${APP_NAME} version ${VERSION}${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Build the Docker image
echo -e "${YELLOW}Building Docker image...${NC}"
docker build -t "${IMAGE_NAME}:${VERSION}" -t "${IMAGE_NAME}:latest" .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Docker image built successfully${NC}"
else
    echo -e "${RED}Failed to build Docker image${NC}"
    exit 1
fi

# Push to registry
echo -e "${YELLOW}Pushing to registry...${NC}"
docker push "${IMAGE_NAME}:${VERSION}"
docker push "${IMAGE_NAME}:latest"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully pushed ${IMAGE_NAME}:${VERSION} to registry${NC}"
    echo -e "${GREEN}Successfully pushed ${IMAGE_NAME}:latest to registry${NC}"
else
    echo -e "${RED}Failed to push to registry${NC}"
    exit 1
fi

# Clean up local images (optional)
read -p "Do you want to remove local images? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Removing local images...${NC}"
    docker rmi "${IMAGE_NAME}:${VERSION}" "${IMAGE_NAME}:latest" || true
    echo -e "${GREEN}Local images removed${NC}"
fi

echo -e "${GREEN}Build and push completed successfully!${NC}"
echo -e "${GREEN}Image: ${IMAGE_NAME}:${VERSION}${NC}"