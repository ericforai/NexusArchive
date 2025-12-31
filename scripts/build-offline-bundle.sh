#!/bin/bash
# Input: Docker Images
# Output: Offline Bundle (.tar.gz)
# Pos: NexusArchive scripts
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

# Configuration
BUNDLE_DIR="dist/offline-bundle"
IMAGE_TAG="latest"
OUTPUT_TAR="nexus-archive-offline.tar.gz"

echo "[INFO] Starting offline bundle build..."

# 1. Clean previous build
rm -rf $BUNDLE_DIR
mkdir -p $BUNDLE_DIR/images

# 2. Pull/Build latest images (Ensure they exist locally)
# In real scenario, we might build here. For now assume they exist or pull them.
echo "[INFO] Saving Docker images..."

# List of images to export
IMAGES=(
    "nexus-core:${IMAGE_TAG}"
    "nexus-ui:${IMAGE_TAG}"
    "postgres:15-alpine"
    "redis:alpine"
)

# Export images to tar files
for img in "${IMAGES[@]}"; do
    filename=$(echo $img | tr ':' '_').tar
    echo "  > Saving $img to $filename..."
    # Note: check if image exists, if not pull or warn
    if [[ "$(docker images -q $img 2> /dev/null)" == "" ]]; then
      echo "  [WARN] Image $img not found locally. Attempting pull..."
      docker pull $img || echo "  [ERROR] Failed to pull $img"
    fi
    docker save -o "$BUNDLE_DIR/images/$filename" $img
done

# 3. Copy deployment configs
echo "[INFO] Copying deployment configs..."
cp deploy/docker-compose.offline.yml $BUNDLE_DIR/docker-compose.yml
# Copy init sql if exists
if [ -d "deploy/sql" ]; then
    cp -r deploy/sql $BUNDLE_DIR/
fi

# 4. Create install script in bundle (Simple loader)
cat > $BUNDLE_DIR/load_images.sh <<EOF
#!/bin/bash
echo "Loading Docker images..."
for img in images/*.tar; do
    echo "Loading \$img..."
    docker load -i "\$img"
done
echo "Images loaded."
EOF
chmod +x $BUNDLE_DIR/load_images.sh

# 5. Compress
echo "[INFO] Compressing bundle..."
cd dist
tar -czf $OUTPUT_TAR offline-bundle
cd ..

echo "[SUCCESS] Offline bundle created at: dist/$OUTPUT_TAR"
