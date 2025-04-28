#!/bin/bash

# Script to run the generatepdf-app Docker container with proper configuration

# Default values
USE_H2=true
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
APP_PORT="8091"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --use-h2)
      USE_H2=true
      shift
      ;;
    --mysql-host)
      MYSQL_HOST="$2"
      shift 2
      ;;
    --mysql-port)
      MYSQL_PORT="$2"
      shift 2
      ;;
    --app-port)
      APP_PORT="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Build the Docker image if it doesn't exist
if [[ "$(docker images -q generatepdf-app 2> /dev/null)" == "" ]]; then
  echo "Building Docker image..."
  docker build -t generatepdf-app .
fi

# Set environment variables based on database choice
if [ "$USE_H2" = true ]; then
  echo "Running with H2 in-memory database..."
  docker run -p ${APP_PORT}:8091 \
    -e SPRING_DATASOURCE_URL=jdbc:h2:mem:pdf \
    -e SPRING_DATASOURCE_USERNAME=sa \
    -e SPRING_DATASOURCE_PASSWORD= \
    -e SPRING_DATASOURCE_DRIVER=org.h2.Driver \
    -e SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect \
    generatepdf-app
else
  echo "Running with MySQL database at ${MYSQL_HOST}:${MYSQL_PORT}..."
  docker run -p ${APP_PORT}:8091 \
    -e MYSQL_HOST=${MYSQL_HOST} \
    -e SPRING_DATASOURCE_URL=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/pdf?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true \
    -e SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-root} \
    -e SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-} \
    -e SPRING_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver \
    -e SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect \
    generatepdf-app
fi
