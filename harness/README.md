This folder contains sample Harness pipeline templates for the Kendrick Labernetes project.

Files:
- `sample-harness-pipeline.yaml` — template showing a minimal CI (build & push) and CD (k8s deploy) flow.

Notes:
- Replace placeholder variables and secret references with your Harness project/organization-specific values.
- Import the YAML into Harness (CI and CD sections) and wire connectors (Docker registry, Kubernetes) and secrets.
- The sample uses simple shell steps to keep the template portable; you may replace them with Harness native steps (DockerBuild, K8sDeploy, etc.) for better tracing and permissions.
