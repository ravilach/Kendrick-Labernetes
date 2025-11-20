> **Tip:**  
> To quickly run Wiz Khalubernetes locally, use the pre-built Docker image:  
> 
> ```sh
> docker run -p 80:80 -p 8080:8080 rlachhman/kendrick-labernetes
> ```
> This will start both the frontend (on port 80) and backend (on port 8080) instantly.
>
> The app will be available at [http://localhost](http://localhost), served by nginx over port 80.


## CI/CD: Build and Push Docker Image with GitHub Actions

This project includes a sample GitHub Actions workflow to build and push the Docker image to DockerHub on every push to `main`.

1. Set up your DockerHub credentials as GitHub repository secrets:
   - `DOCKER_USERNAME`
   - `DOCKER_PASSWORD`
2. On push to `main`, the workflow in `.github/workflows/docker-image.yml` will build and push the image tagged as `kendrick-labernetes:latest`.

## Infrastructure: Provision MongoDB EC2 Instance with Terraform

Sample Terraform configuration is provided in the `terraform/` folder to create an Amazon EC2 instance and install MongoDB.



# Kendrick-Labernetes

A full-stack Spring Boot + React app for sharing Wiz Khalifa quotes, designed for cloud-native deployment and flexible database options.

---

## 1. What the App Is
- **Backend:** Spring Boot (Java 17)
- **Frontend:** React + TypeScript
- **Default DB:** Embedded H2 (local/dev)
- **Production DB:** MongoDB (remote, e.g., EC2)
- **Metrics:** Prometheus endpoint
- **Deployment:** Docker, Kubernetes, GitHub Actions

---

## 2. How to Build Locally (No Docker)

### Backend (Spring Boot)
```sh
cd backend
mvn clean package
java -jar target/kendrick-labernetes-backend-0.0.1-SNAPSHOT.jar
```
- Default: Uses embedded H2 DB
- To use MongoDB, set `spring.data.mongodb.uri` in `src/main/resources/application.properties`

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
# docker run -p 80:80 -p 8080:8080 -e REMOTE_DB=false kendrick-labernetes

# For remote MongoDB
# Example: no auth, using IP address and DB name 'wizquotes'
# docker run -p 80:80 -p 8080:8080 -e REMOTE_DB=true -e MONGODB_URI="mongodb://192.168.1.100:27017/wizquotes" kendrick-labernetes
```
- Access frontend: [http://localhost](http://localhost) (served by nginx on port 80)
- Access backend API: [http://localhost:8080](http://localhost:8080)

> **Note:** If you only map port 8080, the frontend will not be available. Always map port 80 for the UI.
> **Default behavior:** If REMOTE_DB is not set, the app uses local H2 for fastest startup.

---

## 4. Changing the Connection String (Local/Remote MongoDB)

- **Default:** Embedded H2 DB (no config needed)
- **Remote MongoDB:**
   - In `backend/src/main/resources/application.properties`, uncomment the line:
      ```properties
      spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/wizquotes}
      # Uncomment this property to use MongoDB (remote or local). Leave commented to use embedded H2 for local/dev.
      ```
   - Or set as environment variable:
      ```sh
      export SPRING_DATA_MONGODB_URI="mongodb://<username>:<password>@<host>:27017/wizquotes?authSource=admin"
      ```
   - Example for EC2:
      ```properties
      spring.data.mongodb.uri=mongodb://admin:password@ec2-xx-xx-xx-xx.compute.amazonaws.com:27017/wizquotes?authSource=admin
      ```

---

## 5. GitHub Actions Used to Build and Create the Mongo Instance


- **CI/CD:**
   - `.github/workflows/docker-image.yml` builds and pushes Docker images to DockerHub on every push to `main`.
   - Secrets required: `DOCKER_USERNAME`, `DOCKER_PASSWORD`
- **MongoDB Provisioning:**
   - `.github/workflows/terraform-apply.yml` applies Terraform in the `terraform/` folder to provision a MongoDB EC2 instance automatically.
   - Required secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, (optional) `AWS_DEFAULT_REGION`
   - Output: EC2 public IP for MongoDB connection

---

## 6 Switching Between Local DB (H2) and Remote MongoDB
- Use the `REMOTE_DB` environment variable in `deployment.yaml`:
   - `REMOTE_DB: "false"` (default) uses embedded H2 (no external DB required)
   - `REMOTE_DB: "true"` uses remote MongoDB (set `MONGODB_URI` accordingly)
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
   - Set `REMOTE_DB` and `MONGODB_URI` environment variables as needed (see section 6).
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
| `/api/quotes`                 | POST   | Submit a new Wiz Khalifa quote              |
| `/api/quotes/latest`          | GET    | Get the latest quote                        |
| `/api/nodeinfo`               | GET    | Get node/system/application info            |
| `/api/dbstatus`               | GET    | Get current DB connection status/type.      |
| `/actuator/prometheus`        | GET    | Prometheus metrics endpoint                 |

---

## 10. How to Expose the Local App in Docker to Ngrok

You can use [ngrok](https://ngrok.com/) to securely expose your local Wiz Khalubernetes app running in Docker to the internet for demos, testing, or remote access.

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

You can use the following Prometheus queries to monitor Wiz Khalubernetes metrics exposed at `/actuator/prometheus`:

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

## Development Notes
- Backend: Spring Boot (Java 17)
- Frontend: React + TypeScript
- MongoDB: Used for persistence
- Prometheus: Metrics exposed at `/actuator/prometheus`

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

Feel free to customize and extend Kendrick Labernetes!
