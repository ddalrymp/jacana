# jacana

This project demonstrates a small application to highlight a collection of technologies working together in a local (laptop) environment. This application is a REST API running in a Helidon (Java) container. The data stored in the REST API is persisted in a MySQL database (also running in a container). By default logging of the REST API is captured and processed by fluentd. There is a configuration option to enable Elasticsearch and Kibana, and to have fluentd send log data to Elasticsearch.

# Prerequisites and Environment Setup

Machine: MacOS (M1 processor)

[Install Docker Desktop.](https://docs.docker.com/desktop/install/mac-install/)

[Install minikube (for MacOS M1 processor)](https://minikube.sigs.k8s.io/docs/start/?arch=%2Fmacos%2Fx86-64%2Fstable%2Fbinary+download)
```
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-arm64
sudo install minikube-darwin-arm64 /usr/local/bin/minikube
```

Install kubectl
```
brew install kubectl
```
Install helm
```
brew install helm
```
Install Java 21+
```
brew install java
```
Install Maven 3.8+
```
brew install maven
```
Verify versions and tools
```
java -version
mvn --version
docker --version
kubectl version
```


Docker Desktop 
Kubernetes (minikube is used for this project)
Java 21+
Maven (?.?)

Start up Docker Desktop

Start up minikube
```
minikube start --insecure-registry "10.0.0.0/24"
```

Enable the Docker Registry within minikube (one time operation). The command is idempotent so it is ok to re-run.
```
minikube addons enable registry
```

Open a port to the minikube Docker Registry. The command uses localhost:5200, because localhost:5000 is in use by another process:
```
docker run --rm -it --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5200,reuseaddr,fork TCP:$(minikube ip):5000"
```
This command will remain running in the terminal where it is started. The last line of output should start with 'OK:...'. To terminate the command type 'Ctrl+c'

Start the tunnel to minikube so we can access `LoadBalancer` services running in minikube. Only the `LoadBalancer` services are given an External IP in minikube.
```
minikube tunnel
```
You will be prompted for your user's password to access privileged ports 80 and 443

Grant fluentd permission to logging resources
```
kubectl create -f fluentd-rbac.yaml
```

# Quickstart

Build the Docker Image
```
docker build -t helidon-quickstart-mp rest-api/.
```

Push the Docker Image to local image repository
(Remember to forward localhost:5200 to minikube VM on port 5000 - See above Environment Setup for the command)
```
docker tag helidon-quickstart-mp localhost:5200/helidon-quickstart-mp
docker push localhost:5200/helidon-quickstart-mp
```

Install the app using Helm
```
kubectl create namespace jacana
helm install jacana-release k8s/. -n jacana
```

Check the app
```
kubectl get pod -n jacana 
```
You should see all pods in the 'Running' state.

Test the REST API
( Remember to start the minikube tunnel - See above Environment Setup for the command )

# Updating the code

Build the code. This will run the test cases which are skipped when building the Docker image, so this is an important step for validation.
```
cd rest-api
mvn package
```

Build Docker image
```
docker build -t helidon-quickstart-mp rest-api/.
```

Tag and Push the Docker image
```
docker tag helidon-quickstart-mp localhost:5200/helidon-quickstart-mp
docker push localhost:5200/helidon-quickstart-mp
```

Delete the helidon-quickstart-mp pod so that it is recreated with the actual latest image
```
kubectl delete pod $(kubectl get pod -n jacana --no-headers -o custom-columns=":metadata.name" | grep "helidon-quickstart-mp") -n jacana
```
List the pods to see when they are running again
```
kubectl get pod -n jacana
```

Test the REST API endpoint
```
curl -X GET http://localhost:8080/simple-greet
```



Remember to bump the versions in Chart.yaml
```
helm uninstall jacana-release -n jacana
helm install jacana-release k8s/. -n jacana
# helm upgrade jacana-release k8s/. -n jacana
```

Get metrics, json formatted
```
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
```

Get metrics, Prometheus formatted
```
curl -s -X GET http://localhost:8080/metrics
```

Get health
```
curl -s -X GET http://localhost:8080/health
```

