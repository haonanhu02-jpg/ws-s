# Backend dependency download diagnostics

Docker builds explicitly load `deploy/maven-settings.xml`, using the Aliyun
HTTPS mirror for repository ID `central` (including Central plugin dependencies).
Other repository IDs and host Maven settings are not changed. Existing cache
contents are retained. Logs should show `Downloading from aliyun-central`.
Override the build argument `MAVEN_CENTRAL_MIRROR_URL` to use an internal mirror
or `https://repo.maven.apache.org/maven2` instead.

The backend Docker build keeps Maven batch download messages enabled. A stalled
download should leave its repository and artifact URL in the plain build log.
Maven HTTP connection timeout defaults to 10 seconds, read inactivity timeout to
60 seconds, and HTTP retries to at most one retry. These are per-request limits,
not an overall build deadline. Slow downloads that continue receiving data can
take longer. The shared Maven cache remains locked to preserve cache safety.

After pulling the change, build one service first:

```sh
docker compose --progress plain -f deploy/docker-compose.yml build dormitory-service
```

If it fails, retain the last `Downloading from` URL and the error following it.
Do not delete the cache or use `--no-cache` as the first troubleshooting step.
Only restart services after the required images build successfully.

Timeouts can be overridden for a slow but working connection:

```sh
docker compose --progress plain -f deploy/docker-compose.yml build \
  --build-arg MAVEN_CONNECT_TIMEOUT_MS=20000 \
  --build-arg MAVEN_READ_TIMEOUT_MS=120000 dormitory-service
```

Configuration reference: https://maven.apache.org/resolver/configuration.html
