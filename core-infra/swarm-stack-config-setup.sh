#!/bin/bash

# Docker Swarm Stack Configuration Setup Script
# Setup configuration for Prometheus, Traefik, EMQX and Grafana with Docker Swarm
# Includes automated Grafana provisioning for IoT sensor dashboards

set -e

echo "🚀 Starting Docker Swarm Stack Configuration setup..."
echo "This will configure: Traefik, Prometheus, EMQX, and Grafana with IoT dashboards"

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

# Create Grafana configs
echo "Creating Grafana configurations..."

# Function to create or update config
create_grafana_config() {
    local config_name=$1
    local file_path=$2
    
    if [ ! -f "$file_path" ]; then
        echo "Warning: File $file_path does not exist, skipping $config_name"
        return 1
    fi
    
    if docker config ls --format "{{.Name}}" | grep -q "^${config_name}$"; then
        echo "$config_name already exists, removing and recreating..."
        docker config rm $config_name || true
    fi
    
    echo "Creating config: $config_name from $file_path"
    docker config create $config_name $file_path
}

# Create Grafana main config
create_grafana_config "grafana_config" "grafana/grafana.ini"

# Create datasource config
create_grafana_config "grafana_datasource_config" "grafana/provisioning/datasources/influxdb.yml"

# Create dashboard provisioning config
create_grafana_config "grafana_dashboard_config" "grafana/provisioning/dashboards/dashboard.yml"

# Create dashboard files
create_grafana_config "grafana_iot_sensors_dashboard" "grafana/provisioning/dashboards/iot-sensors.json"
create_grafana_config "grafana_iot_overview_dashboard" "grafana/provisioning/dashboards/iot-overview.json"

echo "All Grafana configs created successfully!"
echo "Configs created:"
echo "  ✅ grafana_config (main configuration)"
echo "  ✅ grafana_datasource_config (InfluxDB connection)"
echo "  ✅ grafana_dashboard_config (dashboard provisioning)"
echo "  ✅ grafana_iot_sensors_dashboard (device-specific dashboard)"
echo "  ✅ grafana_iot_overview_dashboard (overview dashboard)"

# Verify all configs exist
echo ""
echo "Verifying all configs are created:"
docker config ls | grep grafana

# Check created configs
echo "Docker configs list:"
docker config ls

# Deploy stack
echo "Deploying Docker Swarm stack..."
docker stack deploy -c docker-compose-infra-swarm.yaml sss-infra

# Wait for services to stabilize
echo "Waiting for services to stabilize..."
sleep 10

# Force update Grafana service to ensure configs are applied
echo "Applying Grafana configurations..."
docker service update --force sss-infra_grafana

# Wait for Grafana to restart with new configs
echo "Waiting for Grafana to restart with new configurations..."
sleep 10

# Verify Grafana service is running
echo "Verifying Grafana service status..."
docker service ps sss-infra_grafana --no-trunc

# Check services status
echo "Checking all services status..."
sleep 5
docker stack services sss-infra

echo ""
echo "🔍 Grafana Service Details:"
docker service inspect sss-infra_grafana --format '{{.Spec.TaskTemplate.ContainerSpec.Configs}}' | head -5

echo ""
echo "🔧 Verifying Grafana Dashboard Configuration:"
# Wait a bit more for Grafana to fully start
sleep 10
echo "Checking if Grafana container is running..."
GRAFANA_CONTAINER=$(docker ps --filter "name=sss-infra_grafana" --format "{{.ID}}" | head -1)
if [ ! -z "$GRAFANA_CONTAINER" ]; then
    echo "Found Grafana container: $GRAFANA_CONTAINER"
    echo "Checking mounted configs in container:"
    docker exec $GRAFANA_CONTAINER ls -la /etc/grafana/provisioning/dashboards/ || echo "Could not access dashboard directory"
    echo "Checking if dashboard files exist:"
    docker exec $GRAFANA_CONTAINER ls -la /etc/grafana/provisioning/dashboards/*.json || echo "No JSON files found"
    echo "Checking Grafana logs for dashboard loading:"
    docker logs $GRAFANA_CONTAINER 2>&1 | grep -i "dashboard\|provision" | tail -5 || echo "No dashboard logs found"
else
    echo "⚠️  Grafana container not found, checking service logs:"
    docker service logs sss-infra_grafana --tail 10
fi

echo "Docker Swarm Stack setup completed!"
echo ""
echo "🚀 Access Information:"
echo "   Web Admin (Main): https://media115.lanestel.fr"
echo "   Grafana Dashboard: https://media115.lanestel.fr/grafana"
echo "   Traefik Dashboard: http://localhost:5050"
echo "   Prometheus: http://localhost:9090"
echo "   EMQX Dashboard: http://localhost:18083 (admin/public)"
echo "   RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo ""
echo "📊 Grafana Configuration Status:"
echo "   - All configs applied: ✅"
echo "   - Service restarted with new configs: ✅"
echo "   - InfluxDB datasource: Auto-configured"
echo "   - IoT dashboards: Pre-loaded"
echo "   - Authentication: SSO via sss-web-admin"
echo "   - Device selection: Available in dashboards"
echo ""
echo "📊 Grafana Information:"
echo "   - Automatically configured with InfluxDB datasource"
echo "   - IoT Sensor dashboards pre-loaded"
echo "   - Authentication via SSO (sss-web-admin)"
echo "   - Device selection dropdown available"
echo ""
echo "🔧 Useful Commands:"
echo "   - View logs: docker service logs sss-infra_<service-name>"
echo "   - Scale service: docker service scale sss-infra_<service-name>=<replicas>"
echo "   - Update service: docker service update sss-infra_<service-name>"
echo "   - Remove stack: docker stack rm sss-infra"
echo "   - Force update Grafana: docker service update --force sss-infra_grafana"
echo "   - Check Grafana configs: docker exec \$(docker ps -q -f name=sss-infra_grafana) ls -la /etc/grafana/provisioning/"
echo "   - Test Grafana datasource: curl -u admin:admin http://localhost:3000/api/datasources"
echo ""
echo "📝 Important Notes:"
echo "   - Ensure swarm nodes have appropriate labels (u24serv-cac, u24serv-db)"
echo "   - Check firewall for ports: 80, 443, 1883, 5050, 9090, 18083"
echo "   - Grafana is accessible only via authentication (no direct access)"
echo "   - IoT sensor data will be automatically available in dashboards"
echo "   - Backup data volumes regularly"
echo "   - All Grafana configs are provisioned automatically"
echo ""
echo "🔍 Troubleshooting:"
echo "   - If Grafana shows no data, check InfluxDB connection"
echo "   - For authentication issues, check sss-web-admin logs"
echo "   - View service details: docker service inspect sss-infra_<service-name>"
echo "   - Check dashboard loading: docker service logs sss-infra_grafana | grep dashboard"
echo "   - Verify configs in container: docker exec \$(docker ps -q -f name=sss-infra_grafana) ls /etc/grafana/provisioning/"
echo "   - Force reload configs: docker service update --force sss-infra_grafana"
echo "   - Check Grafana API: curl -u admin:admin http://localhost:3000/api/dashboards/home"
echo "   - Test dashboard API: curl -u admin:admin http://localhost:2030/api/search?query=IoT"
echo "   - Validate JSON files: python3 -m json.tool grafana/provisioning/dashboards/iot-sensors.json"
echo ""
echo "🚫 Common Dashboard Issues:"
echo "   1. Dashboards not loading: Check if JSON files are properly mounted"
echo "   2. Empty dashboard list: Verify dashboard.yml provisioning config"
echo "   3. Datasource errors: Ensure InfluxDB is accessible from Grafana"
echo "   4. Permission issues: Check Grafana logs for provisioning errors"
echo ""
echo "📋 Next Steps if Dashboards Still Don't Load:"
echo "   1. Run: docker service logs sss-infra_grafana | grep -i 'dashboard\|provision'"
echo "   2. Check config mounting: docker exec \$(docker ps -q -f name=sss-infra_grafana) ls -la /etc/grafana/provisioning/dashboards/"
echo "   3. Validate JSON syntax: python3 -m json.tool grafana/provisioning/dashboards/iot-sensors.json"
echo "   4. Force service update: docker service update --force sss-infra_grafana"
echo "   5. Check provisioning logs: docker exec \$(docker ps -q -f name=sss-infra_grafana) cat /var/log/grafana/grafana.log | grep provision"
echo ""
echo "🌡️ Temperature Data Troubleshooting:"
echo "   1. Check InfluxDB data: docker exec \$(docker ps -q -f name=sss-infra_influxdb) influx query 'from(bucket:\"telemetry-data\") |> range(start: -1h) |> limit(n:10)'"
echo "   2. Test datasource: curl -u admin:admin http://localhost:2030/api/datasources/proxy/1/health"
echo "   3. Check measurements: docker exec \$(docker ps -q -f name=sss-infra_influxdb) influx query 'import \"influxdata/influxdb/schema\" schema.measurements(bucket: \"telemetry-data\")'"
echo "   4. Verify device_ids: docker exec \$(docker ps -q -f name=sss-infra_influxdb) influx query 'from(bucket:\"telemetry-data\") |> range(start: -1h) |> keep(columns: [\"device_id\"]) |> distinct()'"
echo "   5. Check field names: docker exec \$(docker ps -q -f name=sss-infra_influxdb) influx query 'from(bucket:\"telemetry-data\") |> range(start: -1h) |> keep(columns: [\"_field\"]) |> distinct()'"