# Deployment Walkthrough: Local -> Volcano Engine

I have set up a complete "Local Build, Remote Run" deployment pipeline. This ensures your production server stays clean and secure.

### Webhook Encryption & Authorization
- **Problem**:
  1. Cannot find `EncodingAESKey` in YonSuite UI.
  2. API Error 401 when syncing vouchers (`/yonbip/fi/ficloud/openapi/voucher/queryVouchers`).
- **Solution**:
  1. **Encryption**: Integrated `iuap-ip-openapi-sdk` to use `AppKey`/`AppSecret` for decryption.
  2. **Authorization**: Identified **Critical AppKey Mismatch**. 
     - Configured AppKey: `e936...` (Used in code)
     - Authorized AppKey: `96a9...` (Shown in user screenshot)
     - **Action**: User must either update `application.yml` to match the authorized app OR find the correct app (`e936...`) in YonSuite and authorize it.
- **Verification**:
  - Webhook test passed (health check 200).
  - Sync test blocked by authorization mismatch.

## 1. Files Created
- **`deploy/build.sh`**: Automates `npm build` and `mvn package`, creating a `nexusarchive-release.tar.gz`.
- **`deploy/deploy.sh`**: Uploads the tarball to your server and restarts services.
- **`deploy/nginx.conf`**: Production-ready Nginx configuration.
- **`deploy/nexusarchive.service`**: Systemd unit file for the Java backend.
- **`.env.example`**: Template for environment variables.

## 2. One-Time Server Setup
Before the first deployment, SSH into your Volcano Engine server and run:

```bash
# 1. Install Dependencies
apt update && apt install -y openjdk-17-jre nginx postgresql

# 2. Create Directory & User
useradd -r -s /bin/false nexus
mkdir -p /opt/nexusarchive
chown -R nexus:nexus /opt/nexusarchive

# 3. Configure Environment
# Copy content from .env.example to /opt/nexusarchive/.env and edit it
nano /opt/nexusarchive/.env
chmod 600 /opt/nexusarchive/.env

# 4. Install Service
# Copy deploy/nexusarchive.service content to /etc/systemd/system/nexusarchive.service
systemctl daemon-reload
systemctl enable nexusarchive

# 5. Configure Nginx
# Copy deploy/nginx.conf content to /etc/nginx/sites-available/nexusarchive
ln -s /etc/nginx/sites-available/nexusarchive /etc/nginx/sites-enabled/
rm /etc/nginx/sites-enabled/default
systemctl reload nginx
```

## 3. How to Deploy
From your **local machine**:

1.  Edit `deploy/deploy.sh` to set your `SERVER_HOST` (IP address).
2.  Run:
    ```bash
    ./deploy/deploy.sh
    ```

This will:
1.  Build frontend and backend locally.
2.  Upload the compressed artifact.
3.  Backup the old version on the server.
4.  Unpack the new version.
5.  Restart the backend and Nginx.

## 4. Verification
After deployment, check:
- **Frontend**: Visit `http://your-server-ip`
- **Backend**: `curl http://your-server-ip/api/health` (or check logs `journalctl -u nexusarchive -f`)
