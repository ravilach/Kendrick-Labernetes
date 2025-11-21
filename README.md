> **Tip:**  
> To quickly run Kendrick Labernetes locally, use the pre-built Docker image:  
> 
> ```sh
> docker run -p 80:80 -p 8080:8080 rlachhman/kendrick-labernetes
> ```
> This will start both the frontend (on port 80) and backend (on port 8080) instantly.
>
> The app will be available at [http://localhost](http://localhost), served by nginx over port 80.


# CI/CD and Infrastructure (Harness)
You can use Harness for CI/CD — a sample Harness pipeline is provided in `harness/sample-harness-pipeline.yaml`. 

# Kendrick-Labernetes

A full-stack Spring Boot + React app for sharing Kendrick Lamar quotes, designed for cloud-native deployment and flexible database options.

---

## 1. What the App Is
- **Backend:** Spring Boot (Java 17)
- **Frontend:** React + TypeScript
- **Default DB:** Embedded H2 (local/dev)
- **Production DB:** MongoDB (remote, e.g., EC2)
 - **Optional DB:** Postgres (supported via `postgres` profile)
- **Metrics:** Prometheus endpoint
   - **Deployment:** Docker, Kubernetes

---

## 2. How to Build Locally (No Docker)

### Backend (Spring Boot)
```sh
cd backend
mvn clean package
java -jar target/kendrick-labernetes-backend-0.0.1-SNAPSHOT.jar
```
- Default: Uses embedded H2 DB
- To use MongoDB, either set `MONGODB_URI` as an environment variable or enable the `mongo` profile.
   The Mongo-specific fallback is defined in `src/main/resources/application-mongo.properties`.
 - To use Postgres or select the DB explicitly, set the `DB_TYPE` environment variable (supported values: `h2`, `mongo`, `postgres`).
   - Use `DB_TYPE` to choose the database (values: `h2`, `mongo`, `postgres`).
    - Example (Postgres dev using local PG):
       ```sh
       export DB_TYPE=postgres
       export SPRING_PROFILES_ACTIVE=postgres
       export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kendrick
       export SPRING_DATASOURCE_USERNAME=postgres
       export SPRING_DATASOURCE_PASSWORD=postgres
       java -jar target/kendrick-labernetes-backend-0.0.1-SNAPSHOT.jar
       ```

### Frontend (React)
```sh
cd frontend
npm install
npm start
```
- Runs at [http://localhost:3000](http://localhost:3000)

---

## 3. How to Build with Docker on a MacBook

### Build Docker Image
```sh
docker build -t kendrick-labernetes .
```
- For amd64: `docker buildx build --platform linux/amd64 -t kendrick-labernetes .`
- No cache: `docker buildx build --no-cache --platform linux/amd64 -t kendrick-labernetes .`
- Prune images: `docker image prune -f && docker builder prune -f`

### Push Docker Image to DockerHub
```sh
docker tag kendrick-labernetes <your-dockerhub-username>/kendrick-labernetes:latest
docker push <your-dockerhub-username>/kendrick-labernetes:latest
```
- Make sure you are logged in: `docker login`
- Replace `<your-dockerhub-username>` with your DockerHub username


### Run Container
```sh
# For local H2 (default, fast startup)
docker run -p 80:80 -p 8080:8080 kendrick-labernetes
# or explicitly
# docker run -p 80:80 -p 8080:8080 -e DB_TYPE=h2 kendrick-labernetes

# For remote MongoDB
# Example: no auth, using IP address and DB name 'kendrickquotes'
# docker run -p 80:80 -p 8080:8080 -e DB_TYPE=mongo -e MONGODB_URI="mongodb://192.168.1.100:27017/kendrickquotes" kendrick-labernetes

# For Postgres (example)
# Start a local Postgres container (dev):
# docker run --name kl-postgres -e POSTGRES_DB=kendrick -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:15
# Run app with Postgres (use DB_TYPE and profile):
# docker run -p 80:80 -p 8080:8080 -e DB_TYPE=postgres -e SPRING_PROFILES_ACTIVE=postgres -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/kendrick -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=postgres kendrick-labernetes
```
- Access frontend: [http://localhost](http://localhost) (served by nginx on port 80)
- Access backend API: [http://localhost:8080](http://localhost:8080)

> **Note:** If you only map port 8080, the frontend will not be available. Always map port 80 for the UI.
> **Default behavior:** If `DB_TYPE` is not set, the app defaults to `h2` (embedded) for fastest startup.

---

## 4. Changing the Connection String (Local/Remote MongoDB)

   - Mongo settings are provided by `application-mongo.properties` (loaded when the `mongo` profile is active).
     You can also provide the connection string via the `MONGODB_URI` environment variable.
     Example (enable the mongo profile and set the env var):
      ```sh
      export SPRING_PROFILES_ACTIVE=mongo
      export MONGODB_URI="mongodb://<username>:<password>@<host>:27017/kendrickquotes?authSource=admin"
      ```
     The local/default fallback is defined in `backend/src/main/resources/application-mongo.properties`.
   - Example for EC2:
      ```properties
      spring.data.mongodb.uri=mongodb://admin:password@ec2-xx-xx-xx-xx.compute.amazonaws.com:27017/kendrickquotes?authSource=admin
      ```

### Remote Postgres

- To use a remote Postgres instance, set the application to use the Postgres profile and provide the JDBC connection information. Use `DB_TYPE=postgres` and `SPRING_PROFILES_ACTIVE=postgres` so Spring Boot enables the Postgres datasource configuration.

- Example environment variables (EC2-hosted Postgres):
   ```sh
   export DB_TYPE=postgres
   export SPRING_PROFILES_ACTIVE=postgres
   export SPRING_DATASOURCE_URL=jdbc:postgresql://ec2-xx-xx-xx-xx.compute.amazonaws.com:5432/kendrick
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=your_password_here
   ```

- Kubernetes Postgres (on-cluster): when Postgres is deployed into the same Kubernetes namespace, you can point to the service name directly. Example if your Postgres Service is `postgres`:
   ```sh
   # From within the cluster or pods in the same namespace
   export DB_TYPE=postgres
   export SPRING_PROFILES_ACTIVE=postgres
   export SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kendrick
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=your_password_here
   ```

- Kubernetes Postgres (off-cluster / external): when Postgres runs outside the cluster (EC2, RDS, etc.) use the external host or load balancer DNS/IP. Example:
   ```sh
   export DB_TYPE=postgres
   export SPRING_PROFILES_ACTIVE=postgres
   export SPRING_DATASOURCE_URL=jdbc:postgresql://external-postgres-host.example.com:5432/kendrick
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=your_password_here
   ```

- Recommended: do NOT hardcode credentials in manifests. Use Kubernetes `Secret` objects and mount or reference them as environment variables in `deployment.yaml` (via `valueFrom.secretKeyRef`). For example:
   ```yaml
   env:
      - name: SPRING_DATASOURCE_URL
         valueFrom:
            secretKeyRef:
               name: kendrick-postgres-secret
               key: datasource-url
      - name: SPRING_DATASOURCE_USERNAME
         valueFrom:
            secretKeyRef:
               name: kendrick-postgres-secret
               key: username
      - name: SPRING_DATASOURCE_PASSWORD
         valueFrom:
            secretKeyRef:
               name: kendrick-postgres-secret
               key: password
   ```

   This keeps credentials out of source control and lets your CI/CD tooling inject secrets at deploy time.


---

## 5. Harness CI/CD Example

A sample Harness CI/CD pipeline is provided in `harness/sample-harness-pipeline.yaml`. It contains a small CI pipeline that builds the backend (Maven), builds the frontend (npm), builds and pushes a Docker image, and a CD pipeline that applies Kubernetes manifests.

Quick usage notes:
- Review and replace the placeholder variables in `harness/sample-harness-pipeline.yaml` (Docker repo, registry, Harness secret/connector refs, and `KUBECONFIG`).
- Import the CI section into Harness CI and the CD section into Harness CD (or adapt to your account's pipeline templates).
- Recommended: create Harness connectors for your Docker registry and Kubernetes cluster, and store sensitive values (Docker password, kubeconfig) as Harness Secrets.


## 6. Switching Between Local DB (H2) and Remote MongoDB
 - Use the `DB_TYPE` environment variable in `deployment.yaml`:
   - `DB_TYPE: "h2"` (default) uses embedded H2 (no external DB required)
   - `DB_TYPE: "mongo"` uses remote MongoDB (set `MONGODB_URI` accordingly)
   - `DB_TYPE: "postgres"` uses Postgres (provide datasource envs or secrets)
- To switch, edit `deployment.yaml` and redeploy:
   ```sh
   kubectl apply -f deployment.yaml
   ```
- Use Kubernetes secrets for sensitive values

---

## 7. Deploying to Kubernetes and Accessing the App

### Deploy to Kubernetes
1. Edit `deployment.yaml`:
   - Set the `image` field to your built/pushed Docker image.
   - Set `DB_TYPE` and `MONGODB_URI` (when using Mongo) or Postgres datasource envs as needed (see section 6).
2. Apply the deployment:
   ```sh
   kubectl apply -f deployment.yaml
   ```
3. Check pod and service status:
   ```sh
   kubectl get pods
   kubectl get svc
   ```

### Access the App
- After deployment, a LoadBalancer service will expose the app.
- Get the external IP:
   ```sh
   kubectl get svc kendrick-labernetes-lb
   ```
- Access the frontend at:
   `http://<EXTERNAL-IP>`
- Access the backend API at:
   `http://<EXTERNAL-IP>:8080`
- Prometheus metrics:
   `http://<EXTERNAL-IP>:8080/actuator/prometheus`

---


## 8. How to Access notes.txt Locally in Docker and via Remote Kubernetes

### In Docker Container
```sh
docker ps  # Get container ID
docker exec -it <container_id> sh
cat notes.txt
```

### In Kubernetes Pod
```sh
kubectl get pods  # Get pod name
kubectl exec -it <pod_name> -- sh
cat notes.txt
```

This file is included in the container for exercise/demo purposes.

---

## 9. API Endpoints
| Endpoint                      | Method | Description                                 |
|-------------------------------|--------|---------------------------------------------|
| `/api/quotes`                 | POST   | Submit a new Kendrick Lamar quote              |
| `/api/quotes/latest`          | GET    | Get the latest quote                        |
| `/api/nodeinfo`               | GET    | Get node/system/application info            |
| `/api/dbstatus`               | GET    | Get current DB connection status/type.      |
| `/actuator/prometheus`        | GET    | Prometheus metrics endpoint                 |

---

## 10. How to Expose the Local App in Docker to Ngrok

You can use [ngrok](https://ngrok.com/) to securely expose your local Kendrick Labernetes app running in Docker to the internet for demos, testing, or remote access.

### Step-by-Step

1. **Start your Docker container**
   ```sh
   docker run -p 80:80 -p 8080:8080 kendrick-labernetes
   ```
   - This exposes the frontend on port 80 and backend API on port 8080.

2. **Install ngrok**
   - Download from https://ngrok.com/download
   - Unzip and move to your PATH (e.g., `/usr/local/bin`)

3. **Expose the frontend (port 80) with ngrok**
   ```sh
   ngrok http 80
   ```
   - This will give you a public HTTPS URL forwarding to your local frontend.

4. **Expose the backend API (port 8080) with ngrok**
   ```sh
   ngrok http 8080
   ```
   - This will give you a public HTTPS URL forwarding to your backend API.

5. **Share the ngrok URLs**
   - Use the provided URLs to access your app from anywhere.
   - Example output:
     ```
     Forwarding https://random-id.ngrok.io -> http://localhost:80
     Forwarding https://another-id.ngrok.io -> http://localhost:8080
     ```

### Notes
- You can run multiple ngrok tunnels at once for frontend and backend.
- For advanced usage (custom domains, reserved URLs, auth), see the [ngrok docs](https://ngrok.com/docs).
- Make sure your firewall allows incoming connections to the mapped ports.

---

## 11. Sample Prometheus Queries

You can use the following Prometheus queries to monitor Kendrick Labernetes metrics exposed at `/actuator/prometheus`:

- **Total quotes created in MongoDB:**
  ```prometheus
  sum(db_mongo_create_total)
  ```
- **Total quotes created in H2:**
  ```prometheus
  sum(db_h2_create_total)
  ```
- **Total quotes deleted in MongoDB:**
  ```prometheus
  sum(db_mongo_delete_total)
  ```
- **JVM memory usage (MB):**
  ```prometheus
  jvm_memory_used_bytes / 1024 / 1024
  ```
- **Node health (custom metric):**
  ```prometheus
  node_health_free_memory_mb
  ```

For more advanced queries and dashboarding, consider integrating with Grafana.


---

## 12. How to Change the Logging Level

You can control the verbosity of application logs using the `logging.level` property in `backend/src/main/resources/application.properties`:

```properties
# Set log level for all classes under com.kendricklabernetes
logging.level.com.kendricklabernetes=INFO
# Levels: TRACE, DEBUG, INFO, WARN, ERROR
```

Change `INFO` to `DEBUG`, `WARN`, `ERROR`, or `TRACE` as needed to adjust the logging verbosity.

---

## 13. Troubleshooting

Quick actionable tips for common problems when running Kendrick Labernetes locally or in Kubernetes.

- Backend fails to start / cannot connect to DB:
   - Verify `DB_TYPE` is set to the intended value (`h2` | `mongo` | `postgres`). If unset, the app defaults to `h2`.
   - For Postgres make sure `SPRING_PROFILES_ACTIVE=postgres` is set so Spring Boot enables the Postgres profile.
   - Check the JDBC or Mongo URI values and credentials. Example checks:
      ```sh
      # Check an environment variable your process sees
      echo "$SPRING_DATASOURCE_URL"
      # Test PostgreSQL connectivity from the host
      pg_isready -h <host> -p 5432
      # Test Mongo connectivity from the host
      mongo --quiet --eval 'db.runCommand({ ping: 1 })' "$MONGODB_URI"
      ```
   - In Kubernetes, inspect pod events and logs:
      ```sh
      kubectl describe pod <pod-name>
      kubectl logs <pod-name> -c kendrick-labernetes
      ```

- Frontend build errors (TypeScript/React):
   - Ensure Node.js and npm versions match project expectations (use `nvm` if needed).
      ```sh
      node -v && npm -v
      cd frontend
      npm install
      npm run build
      ```
   - If a compile error references `Admin.tsx` or duplicate identifiers, ensure there is only one `Admin.tsx` file in `src/` and no conflicting exports.

- App cannot reach backend API (CORS / ports / nginx):
   - If running with Docker locally, map both ports: `-p 80:80 -p 8080:8080` so the frontend (nginx) and backend are exposed.
   - For direct backend access, use `http://localhost:8080` and check `curl http://localhost:8080/api/quotes`.
   - In Kubernetes, if `Service` is a LoadBalancer, check `kubectl get svc` for external IP.

- Prometheus metrics not scraping:
   - Verify pod annotations exist (`prometheus.io/scrape`, `prometheus.io/path`, `prometheus.io/port`) and the `Service` exposes port 8080.
   - Confirm `/actuator/prometheus` returns metrics:
      ```sh
      curl http://<pod-or-service-ip>:8080/actuator/prometheus | head
      ```

- Admin endpoints / connection tests:
   - Use the Admin UI (`Show Admin`) or the API paths under `/api/admin/*` to test DB connectivity and run read-only SQL/Mongo explorers.
   - Note: calling `/api/admin/set-db-type` only records the requested type in the app — a redeploy/restart with the chosen `DB_TYPE` and profile is required to switch the active persistence layer.

- Debugging in Docker or Kubernetes:
   - Docker:
      ```sh
      docker ps
      docker logs <container-id>
      docker exec -it <container-id> sh
      ```
   - Kubernetes port-forwarding for quick local access to the backend:
      ```sh
      kubectl port-forward svc/kendrick-labernetes-lb 8080:8080
      # then open http://localhost:8080
      ```

- Permission / credential issues:
   - Check that secrets are mounted correctly and that your CI/CD or K8s Secret contains the right keys (e.g. `datasource-url`, `username`, `password` used in the README examples).

- Still stuck? Collect useful diagnostic output to share:
   - `kubectl describe pod <pod-name>`
   - `kubectl logs <pod-name> -c kendrick-labernetes` or `docker logs <container-id>`
   - Backend startup logs (look for datasource or bean initialization errors)
   - Frontend build stdout/stderr

---

Feel free to customize and extend Kendrick Labernetes!
