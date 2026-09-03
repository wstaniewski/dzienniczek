#!/usr/bin/env bash

# === LOAD CONFIG ===
if [ ! -f "./cloud-deploy.conf" ]; then
  echo "❌ Missing cloud-deploy.conf. Create it and try again."
  exit 1
fi

source ./cloud-deploy.conf

FULL_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${IMAGE_NAME}"

echo ">> Using project: ${PROJECT_ID}"
gcloud config set project "${PROJECT_ID}"

# === CHECK DOCKER DAEMON ===
echo ">> Checking Docker daemon..."
if ! docker info >/dev/null 2>&1; then
    echo "⚠ Docker is not running. Starting Docker Desktop..."
    open -a Docker

    for i in {1..30}; do
        if docker info >/dev/null 2>&1; then
            echo "✔ Docker is now running."
            break
        fi
        echo "   Waiting for Docker... ($i)"
        sleep 1
    done
fi

if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker daemon still not running. Attempting fallback build..."
fi

# === BUILD JAR ===
echo ">> Building JAR..."
mvn clean package -DskipTests

# === BUILD DOCKER IMAGE ===
echo ">> Building Docker image: ${FULL_IMAGE}"
docker build -t "${FULL_IMAGE}" .

# === PUSH TO ARTIFACT REGISTRY ===
echo ">> Pushing image to Artifact Registry..."
docker push "${FULL_IMAGE}"

# === DEPLOY TO CLOUD RUN ===
echo ">> Deploying to Cloud Run..."
gcloud run deploy "${SERVICE_NAME}" \
  --image "${FULL_IMAGE}" \
  --platform managed \
  --region "${REGION}" \
  --allow-unauthenticated \
  --port "${PORT}" \
  --memory "${MEMORY}" \
  --cpu "${CPU}"

echo ">> Deployment complete!"