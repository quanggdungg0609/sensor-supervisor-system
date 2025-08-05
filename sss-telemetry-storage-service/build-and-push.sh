#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration - No defaults, must be provided
REGISTRY_URL="${REGISTRY_URL:-}"
REGISTRY_USERNAME="${REGISTRY_USERNAME:-}"
REGISTRY_PASSWORD="${REGISTRY_PASSWORD:-}"
IMAGE_GROUP="${IMAGE_GROUP:-quangdung}"
IMAGE_NAME="${IMAGE_NAME:-sss-telemetry-storage-service}"
NATIVE_BUILD="${NATIVE_BUILD:-false}"

# Functions
print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --registry URL          Registry URL (REQUIRED)"
    echo "  --username USER         Registry username (REQUIRED)"
    echo "  --password PASS         Registry password (REQUIRED)"
    echo "  --group GROUP           Image group/namespace (default: quangdung)"
    echo "  --name NAME             Image name (default: sss-data-ingestor-service)"
    echo "  --native                Build native image (requires GraalVM)"
    echo "  --no-push               Build image but don't push to registry"
    echo "  --no-compose-update     Don't update docker-compose.yaml"
    echo "  --help                  Show this help message"
    echo ""
    echo "Environment Variables (REQUIRED):"
    echo "  REGISTRY_URL            Registry URL"
    echo "  REGISTRY_USERNAME       Registry username"
    echo "  REGISTRY_PASSWORD       Registry password"
    echo "  IMAGE_GROUP             Image group/namespace (optional)"
    echo "  IMAGE_NAME              Image name (optional)"
    echo "  NATIVE_BUILD            Build native image (true/false, optional)"
    echo ""
    echo "Examples:"
    echo "  # Build JVM image"
    echo "  $0 --registry your-registry.com --username user --password pass"
    echo ""
    echo "  # Build native image"
    echo "  $0 --native --registry your-registry.com --username user --password pass"
    echo ""
    echo "  # Build native image locally only"
    echo "  $0 --native --no-push"
}

# Validate required parameters
validate_credentials() {
    local missing_params=()
    
    if [ -z "$REGISTRY_URL" ]; then
        missing_params+=("REGISTRY_URL")
    fi
    
    if [ -z "$REGISTRY_USERNAME" ]; then
        missing_params+=("REGISTRY_USERNAME")
    fi
    
    if [ -z "$REGISTRY_PASSWORD" ]; then
        missing_params+=("REGISTRY_PASSWORD")
    fi
    
    if [ ${#missing_params[@]} -gt 0 ]; then
        print_error "Missing required parameters:"
        for param in "${missing_params[@]}"; do
            echo "  - $param"
        done
        echo ""
        print_error "Please provide registry credentials using one of these methods:"
        echo "  1. Environment variables: export REGISTRY_URL=... REGISTRY_USERNAME=... REGISTRY_PASSWORD=..."
        echo "  2. Command line arguments: --registry ... --username ... --password ..."
        echo "  3. Use --help for more information"
        echo ""
        exit 1
    fi
}

# Check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
        print_error "Maven or Maven Wrapper not found!"
        exit 1
    fi
}

# Check if Docker is running
check_docker() {
    if ! docker info &> /dev/null; then
        print_error "Docker is not running or not accessible!"
        exit 1
    fi
}

# Check GraalVM for native builds
check_graalvm() {
    if [ "$NATIVE_BUILD" = "true" ]; then
        if ! command -v native-image &> /dev/null; then
            print_warning "GraalVM native-image not found. Trying container build..."
            print_warning "Make sure Docker has enough memory (8GB+ recommended for native builds)"
        else
            print_success "GraalVM native-image found"
        fi
    fi
}

# Get project version from pom.xml
get_version() {
    if [ -f "./mvnw" ]; then
        VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
    else
        VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
    fi
    
    if [ -z "$VERSION" ]; then
        print_error "Could not extract version from pom.xml"
        exit 1
    fi
    
    echo $VERSION
}

# Login to registry
login_registry() {
    print_step "Logging into registry: $REGISTRY_URL"
    
    echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY_URL" -u "$REGISTRY_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        print_error "Failed to login to registry"
        print_error "Please check your credentials:"
        echo "  Registry: $REGISTRY_URL"
        echo "  Username: $REGISTRY_USERNAME"
        echo "  Password: [HIDDEN]"
        exit 1
    fi
    
    print_success "Successfully logged into registry"
}

# Logout from registry
logout_registry() {
    print_step "Logging out from registry"
    docker logout "$REGISTRY_URL"
}

# Build application
build_app() {
    if [ "$NATIVE_BUILD" = "true" ]; then
        print_step "Building native application (this may take 5-10 minutes)..."
        build_native_app
    else
        print_step "Building JVM application..."
        build_jvm_app
    fi
}

# Build JVM application
build_jvm_app() {
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -Dquarkus.docker.buildx.platform=linux/amd64
    else
        mvn clean package -DskipTests -Dquarkus.docker.buildx.platform=linux/amd64
    fi
    
    if [ $? -ne 0 ]; then
        print_error "JVM application build failed!"
        exit 1
    fi
    
    print_success "JVM application built successfully"
}

# Build native application
build_native_app() {
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -Dnative -DskipTests -Dquarkus.native.container-build=true
    else
        mvn clean package -Dnative -DskipTests -Dquarkus.native.container-build=true 
    fi
    
    if [ $? -ne 0 ]; then
        print_error "Native application build failed!"
        print_error "Make sure you have:"
        echo "  1. GraalVM installed OR Docker with enough memory (8GB+)"
        echo "  2. Native build dependencies installed"
        echo "  3. Sufficient disk space for native compilation"
        exit 1
    fi
    
    print_success "Native application built successfully"
}

# Build Docker image
build_image() {
    local version=$1
    local image_tag="${IMAGE_GROUP}/${IMAGE_NAME}:${version}"
    local latest_tag="${IMAGE_GROUP}/${IMAGE_NAME}:latest"
    
    if [ "$NATIVE_BUILD" = "true" ]; then
        image_tag="${image_tag}-native"
        latest_tag="${latest_tag}-native"
        print_step "Building native Docker image: $image_tag"
    else
        print_step "Building JVM Docker image: $image_tag"
    fi
    
    # Build with Quarkus container image extension
    local build_args="-Dquarkus.container-image.build=true"
    build_args="$build_args -Dquarkus.container-image.group=${IMAGE_GROUP}"
    build_args="$build_args -Dquarkus.container-image.name=${IMAGE_NAME}"
    
    if [ "$NATIVE_BUILD" = "true" ]; then
        build_args="$build_args -Dquarkus.container-image.tag=${version}-native"
        build_args="$build_args -Dnative"
        build_args="$build_args -Dquarkus.native.container-build=true"
    else
        build_args="$build_args -Dquarkus.container-image.tag=${version}"
    fi
    
    if [ -f "./mvnw" ]; then
        ./mvnw package $build_args -DskipTests -Dquarkus.docker.buildx.platform=linux/amd64
    else
        mvn package $build_args -DskipTests -Dquarkus.docker.buildx.platform=linux/amd64
    fi
    
    if [ $? -ne 0 ]; then
        print_error "Docker image build failed!"
        exit 1
    fi
    
    # Tag as latest
    docker tag $image_tag $latest_tag
    
    print_success "Docker image built: $image_tag"
    print_success "Tagged as latest: $latest_tag"
}

# Push image to registry
push_image() {
    local version=$1
    local image_tag="${IMAGE_GROUP}/${IMAGE_NAME}:${version}"
    local latest_tag="${IMAGE_GROUP}/${IMAGE_NAME}:latest"
    
    if [ "$NATIVE_BUILD" = "true" ]; then
        image_tag="${image_tag}-native"
        latest_tag="${latest_tag}-native"
    fi
    
    # Add registry prefix
    local registry_image_tag="${REGISTRY_URL}/${image_tag}"
    local registry_latest_tag="${REGISTRY_URL}/${latest_tag}"
    
    print_step "Tagging for registry: $REGISTRY_URL"
    docker tag $image_tag $registry_image_tag
    docker tag $latest_tag $registry_latest_tag
    
    print_step "Pushing image: $registry_image_tag"
    docker push $registry_image_tag
    
    if [ $? -ne 0 ]; then
        print_error "Failed to push image: $registry_image_tag"
        exit 1
    fi
    
    print_step "Pushing latest tag: $registry_latest_tag"
    docker push $registry_latest_tag
    
    if [ $? -ne 0 ]; then
        print_error "Failed to push latest tag: $registry_latest_tag"
        exit 1
    fi
    
    print_success "Images pushed successfully!"
}

# Update docker-compose.yaml
update_compose() {
    local version=$1
    local image_tag="${REGISTRY_URL}/${IMAGE_GROUP}/${IMAGE_NAME}:${version}"
    
    if [ "$NATIVE_BUILD" = "true" ]; then
        image_tag="${image_tag}-native"
    fi
    
    if [ -f "docker-compose.yaml" ]; then
        print_step "Updating docker-compose.yaml with version: $version"
        
        # Create backup
        cp docker-compose.yaml docker-compose.yaml.bak
        
        # Update image tag
        sed -i.tmp "s|image: .*/${IMAGE_NAME}:.*|image: ${image_tag}|g" docker-compose.yaml
        rm docker-compose.yaml.tmp 2>/dev/null
        
        print_success "docker-compose.yaml updated"
    else
        print_warning "docker-compose.yaml not found, skipping update"
    fi
}

# Main execution
main() {
    echo "=== Data Ingestor Service - Docker Image Build and Push Script ==="
    echo
    
    # Parse arguments
    PUSH_IMAGE=true
    UPDATE_COMPOSE=true
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --registry)
                REGISTRY_URL="$2"
                shift 2
                ;;
            --username)
                REGISTRY_USERNAME="$2"
                shift 2
                ;;
            --password)
                REGISTRY_PASSWORD="$2"
                shift 2
                ;;
            --group)
                IMAGE_GROUP="$2"
                shift 2
                ;;
            --name)
                IMAGE_NAME="$2"
                shift 2
                ;;
            --native)
                NATIVE_BUILD="true"
                shift
                ;;
            --no-push)
                PUSH_IMAGE=false
                shift
                ;;
            --no-compose-update)
                UPDATE_COMPOSE=false
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
    
    # Validate credentials only if pushing
    if [ "$PUSH_IMAGE" = true ]; then
        validate_credentials
    else
        print_warning "Skipping credential validation (--no-push specified)"
    fi
    
    # Show configuration
    print_step "Configuration:"
    if [ "$PUSH_IMAGE" = true ]; then
        echo "  Registry: $REGISTRY_URL"
        echo "  Username: $REGISTRY_USERNAME"
        echo "  Password: [HIDDEN]"
    else
        echo "  Push: DISABLED"
    fi
    echo "  Image Group: $IMAGE_GROUP"
    echo "  Image Name: $IMAGE_NAME"
    echo "  Native Build: $NATIVE_BUILD"
    echo
    
    # Pre-flight checks
    check_maven
    check_docker
    check_graalvm
    
    # Get version
    VERSION=$(get_version)
    print_step "Project version: $VERSION"
    
    # Build application
    build_app
    
    # Build Docker image
    build_image $VERSION
    
    # Login and push to registry
    if [ "$PUSH_IMAGE" = true ]; then
        login_registry
        push_image $VERSION
        logout_registry
    else
        print_warning "Skipping image push (--no-push specified)"
    fi
    
    # Update docker-compose.yaml
    if [ "$UPDATE_COMPOSE" = true ] && [ "$PUSH_IMAGE" = true ]; then
        update_compose $VERSION
    else
        print_warning "Skipping docker-compose.yaml update"
    fi
    
    echo
    print_success "Build and push completed successfully!"
    
    if [ "$PUSH_IMAGE" = true ]; then
        if [ "$NATIVE_BUILD" = "true" ]; then
            echo "Native Image: ${REGISTRY_URL}/${IMAGE_GROUP}/${IMAGE_NAME}:${VERSION}-native"
            echo "Native Latest: ${REGISTRY_URL}/${IMAGE_GROUP}/${IMAGE_NAME}:latest-native"
        else
            echo "JVM Image: ${REGISTRY_URL}/${IMAGE_GROUP}/${IMAGE_NAME}:${VERSION}"
            echo "JVM Latest: ${REGISTRY_URL}/${IMAGE_GROUP}/${IMAGE_NAME}:latest"
        fi
    else
        if [ "$NATIVE_BUILD" = "true" ]; then
            echo "Local Native Image: ${IMAGE_GROUP}/${IMAGE_NAME}:${VERSION}-native"
            echo "Local Native Latest: ${IMAGE_GROUP}/${IMAGE_NAME}:latest-native"
        else
            echo "Local JVM Image: ${IMAGE_GROUP}/${IMAGE_NAME}:${VERSION}"
            echo "Local JVM Latest: ${IMAGE_GROUP}/${IMAGE_NAME}:latest"
        fi
    fi
}

# Run main function
main "$@"