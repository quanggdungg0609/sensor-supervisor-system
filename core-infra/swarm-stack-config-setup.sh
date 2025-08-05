#!/bin/bash

# Docker Swarm Stack Configuration Setup Script
# Setup configuration for Prometheus, Traefik and EMQX with Docker Swarm

set -e

echo "Starting Docker Swarm Stack Configuration setup..."

# Check if Docker Swarm is already initialized
if ! docker info | grep -q "Swarm: active"; then
    echo "Docker Swarm not initialized. Initializing..."
    docker swarm init
else
    echo "Docker Swarm already initialized"
fi

# Create Docker configs for services
echo "Creating Docker configs..."

# Create Traefik config
echo "Creating Traefik config..."
if docker config ls | grep -q "traefik_config"; then
    echo "traefik_config already exists, removing and recreating..."
    docker config rm traefik_config || true
fi
docker config create traefik_config traefik.yml
echo "Traefik config created"

# Create Prometheus config
echo "Creating Prometheus config..."
if docker config ls | grep -q "prometheus_config"; then
    echo "prometheus_config already exists, removing and recreating..."
    docker config rm prometheus_config || true
fi
docker config create prometheus_config prometheus.yml
echo "Prometheus config created"

# Create EMQX config
echo "Creating EMQX config..."
if docker config ls | grep -q "emqx_config"; then
    echo "emqx_config already exists, removing and recreating..."
    docker config rm emqx_config || true
fi
docker config create emqx_config emqx/etc/emqx.conf
echo "EMQX config created"

# Check created configs
echo "Docker configs list:"
docker config ls

# Deploy stack
echo "Deploying Docker Swarm stack..."
docker stack deploy -c docker-compose-infra-swarm.yaml sss-infrastructure

# Check services status
echo "Checking services status..."
sleep 10
docker stack services sss-infrastructure

echo "Docker Swarm Stack setup completed!"
echo ""
echo "Access Information:"
echo "   Traefik Dashboard: http://localhost:5050"
echo "   Prometheus: http://localhost:9090"
echo "   Grafana: http://localhost:3000 (admin/admin)"
echo "   EMQX Dashboard: http://localhost:18083 (admin/public)"
echo "   RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo ""
echo "🔧 Useful Commands:"
echo "   - View logs: docker service logs sss-infrastructure_<service-name>"
echo "   - Scale service: docker service scale sss-infrastructure_<service-name>=<replicas>"
echo "   - Update service: docker service update sss-infrastructure_<service-name>"
echo "   - Remove stack: docker stack rm sss-infrastructure"
echo ""
echo "Notes:"
echo "   - Ensure swarm nodes have appropriate labels (u24serv-cac, u24serv-db)"
echo "   - Check firewall for ports: 80, 443, 1883, 5050, 9090, 3000, 18083"
echo "   - Backup data volumes regularly"