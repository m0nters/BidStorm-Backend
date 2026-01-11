# ELK Stack Setup Guide

This guide explains how to set up and use the ELK (Elasticsearch, Logstash, Kibana) stack for centralized logging in the Online Auction application.

## Overview

The ELK stack provides powerful log aggregation, analysis, and visualization capabilities:

- **Elasticsearch**: Stores and indexes application logs
- **Logstash**: Collects, parses, and transforms logs
- **Kibana**: Provides a web UI for searching and visualizing logs
- **Filebeat**: Ships logs from the application to Logstash

## Architecture

```
Spring Boot Application
        ↓ (writes JSON logs)
    app.log file
        ↓ (monitored by)
    Filebeat
        ↓ (ships to)
    Logstash
        ↓ (parses and sends to)
  Elasticsearch
        ↓ (visualized in)
     Kibana
```

## Prerequisites

- Docker and Docker Compose installed
- At least 4GB RAM allocated to Docker
- Spring Boot application running and generating logs in `app.log`

## Setup Instructions

### 1. Start the ELK Stack

Navigate to the project directory and start the ELK stack:

```powershell
docker-compose -f docker-compose.elk.yml up -d
```

This will start all four services:
- Elasticsearch on port 9200
- Logstash on port 5044 (Beats input)
- Kibana on port 5601
- Filebeat (running as a service)

### 2. Verify Services are Running

Check that all containers are healthy:

```powershell
docker-compose -f docker-compose.elk.yml ps
```

All services should show as "healthy" after a few minutes.

#### Verify Elasticsearch

```powershell
curl http://localhost:9200
```

You should see JSON response with cluster information.

#### Verify Kibana

Open your browser and navigate to:
```
http://localhost:5601
```

Kibana UI should load (may take 1-2 minutes on first startup).

### 3. Configure Kibana Index Pattern

Once Kibana is accessible:

1. Navigate to **Management** → **Stack Management** → **Index Patterns**
2. Click **Create index pattern**
3. Enter pattern: `logstash-online-auction-*`
4. Click **Next step**
5. Select **@timestamp** as the time field
6. Click **Create index pattern**

### 4. View Logs in Kibana

1. Navigate to **Analytics** → **Discover**
2. Select the `logstash-online-auction-*` index pattern
3. You should see your application logs streaming in

## Using Kibana

### Searching Logs

Use the search bar at the top to search logs. Examples:

- Search by log level:
  ```
  log_level: "ERROR"
  ```

- Search by logger name:
  ```
  logger: "com.taitrinh.online_auction.service.ProductService"
  ```

- Search for specific text in messages:
  ```
  log_message: "exception"
  ```

- Combine filters:
  ```
  log_level: "ERROR" AND logger: "*.service.*"
  ```

### Filtering Logs

Click on any field value to add it as a filter:
- Click the **+** icon to filter FOR that value
- Click the **-** icon to filter OUT that value

### Available Fields

The logs include the following fields:
- `@timestamp`: Log timestamp
- `log_level`: Log level (DEBUG, INFO, WARN, ERROR)
- `logger`: Logger name (class name)
- `log_message`: Log message
- `thread`: Thread name
- `stack_trace`: Stack trace (for exceptions)
- `application`: Application name (online-auction)
- `environment`: Environment (development/production)

### Time Range

Use the time picker in the top-right to select:
- Last 15 minutes
- Last 1 hour
- Last 24 hours
- Custom time range

### Creating Visualizations

1. Navigate to **Analytics** → **Visualize Library**
2. Click **Create visualization**
3. Choose visualization type (Line chart, Bar chart, Pie chart, etc.)
4. Select your index pattern
5. Configure metrics and buckets
6. Save your visualization

Example visualizations:
- **Error rate over time**: Line chart showing count of ERROR logs
- **Logs by level**: Pie chart showing distribution of log levels
- **Top error messages**: Data table showing most common errors

### Creating Dashboards

1. Navigate to **Analytics** → **Dashboard**
2. Click **Create dashboard**
3. Click **Add** to add visualizations
4. Arrange and resize as needed
5. Click **Save** to save your dashboard

## Log Structure

The application outputs logs in JSON format with the following structure:

```json
{
  "@timestamp": "2026-01-11T18:42:00.123+07:00",
  "level": "INFO",
  "logger_name": "com.taitrinh.online_auction.service.ProductService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Creating new product",
  "application": "online-auction",
  "environment": "development"
}
```

## Troubleshooting

### No logs appearing in Kibana

1. **Check Filebeat is reading the log file:**
   ```powershell
   docker logs elk-filebeat
   ```
   Look for messages about harvesting files.

2. **Check Logstash is receiving data:**
   ```powershell
   docker logs elk-logstash
   ```
   Look for pipeline started messages.

3. **Check Elasticsearch indices:**
   ```powershell
   curl http://localhost:9200/_cat/indices?v
   ```
   You should see `logstash-online-auction-*` indices.

4. **Verify app.log has content:**
   ```powershell
   Get-Content app.log -Tail 10
   ```

### Elasticsearch container fails to start

- **Increase Docker memory**: Ensure Docker has at least 4GB RAM allocated
- **Check disk space**: Elasticsearch needs sufficient disk space
- **View logs**:
  ```powershell
  docker logs elk-elasticsearch
  ```

### Kibana shows "Kibana server is not ready yet"

- Wait 1-2 minutes for Kibana to fully start
- Check if Elasticsearch is healthy:
  ```powershell
  docker-compose -f docker-compose.elk.yml ps
  ```

### Clear all logs and start fresh

```powershell
# Stop all services
docker-compose -f docker-compose.elk.yml down -v

# Remove data volumes
docker volume rm online-auction_elk-elasticsearch-data
docker volume rm online-auction_elk-filebeat-data

# Start fresh
docker-compose -f docker-compose.elk.yml up -d
```

## Production Considerations

When deploying to production:

1. **Enable Elasticsearch Security**:
   - Set `xpack.security.enabled: true`
   - Configure authentication and TLS

2. **Adjust Memory Settings**:
   - Increase heap sizes based on log volume
   - Monitor resource usage

3. **Configure Log Retention**:
   - Use Index Lifecycle Management (ILM)
   - Set policies for log rotation and deletion

4. **Backup**:
   - Configure snapshot repositories
   - Schedule regular backups

5. **Monitoring**:
   - Enable X-Pack monitoring
   - Set up alerts for critical issues

6. **Scale**:
   - Use multiple Elasticsearch nodes
   - Add more Logstash instances for high throughput

## Stopping the ELK Stack

To stop all ELK services:

```powershell
docker-compose -f docker-compose.elk.yml down
```

To stop and remove all data volumes:

```powershell
docker-compose -f docker-compose.elk.yml down -v
```

## Resources

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Filebeat Documentation](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)
