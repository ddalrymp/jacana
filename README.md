# jacana

This project demonstrates a small application to highlight a collection of technologies working together in a local (laptop) environment. This application is a REST API running in a Helidon (Java) container. The data stored in the REST API is persisted in a MySQL database (also running in a container). Logging of the REST API is captured and processed by fluentd. There is a configuration option to enable Elasticsearch and Kibana, and to have fluentd send log data to Elasticsearch. By default Elasticsearch and Kibana are enabled.

# Prerequisites and Environment Setup

Machine: MacOS (Apple silicon processor)

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

**Optional**, Install jq (it helps a lot and testing commands use it below)
```
brew install jq
```

# Start Up

Start docker desktop

[Start minikube](https://minikube.sigs.k8s.io/docs/handbook/registry/ ):
```
minikube start --insecure-registry "10.0.0.0/24"
```
**or** you can start with a clean minikube
```
minikube delete && minikube start --insecure-registry "10.0.0.0/24"
```
Configure minikube for its own registry (one time operation, but it is idempotent)
```
minikube addons enable registry
```

**Open two terminals to keep the following two commands running.**

Trick docker into some port forwarding so that my localhost:5200 (because my OS has something using port 5000).
```
docker run --rm -it --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5200,reuseaddr,fork TCP:$(minikube ip):5000"
```
Create a tunnel into minikube to access services in the cluster
```
minikube tunnel
```
To terminate either of the two commands above type Ctrl+C in the terminal.

# Deploy the project

Clone the project
```
git clone git@github.com:ddalrymp/jacana.git
cd jacana
```
Create the namespace
```
kubectl create namespace jacana
```
You can verify with
```
kubectl get namespace
```
Build the REST API Docker Image (execute from project root). Should take under a minute to build.
```
docker build -t jacana-rest-api rest-api/.
```
Tag and push the REST API Docker Image to the minikube Docker Registry
```
docker tag jacana-rest-api localhost:5200/jacana-rest-api
docker push localhost:5200/jacana-rest-api
```
Install using Helm
```
helm install jacana-release k8s/. -n jacana
```
**Note** Kibana will enter the Running state, but will not be accessible for at least 1-2 minutes while it starts up internally.

In a separate terminal you can **optionally** monitor the progress of the jacana namespace. There should be four pods (MySQL, Elasticsearch, Kibana, and the REST Application). Four services, two will be a LoadBalancer to access the REST API and Kibana.
```
watch -n 10 'kubectl get all -n jacana'
```
In another terminal you can **optionally** monitor the progress of the kube-system namespace. The log collection tool, fluentd, is deployed to kube-system - for reasons that aren’t perfectly clear I couldn’t get fluentd to run in the jacana namespace. You want to be sure there is a fluentd pod, and a DaemonSet (daemonset.apps/fluentd)
```
watch -n 10 'kubectl get all -n kube-system'
```

# Testing

Perform a quick test to see if the REST API is accessible. This should return an empty array.
```
curl -s -X GET http://localhost:8080/customers | jq
```
Let's insert a simple customer record
```
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers | jq
```
List the customers again and see the newly added customer.
```
curl -s -X GET http://localhost:8080/customers | jq
```

## Observability

### Watch the logs in fluentd

Open a terminal and run this command:
```
kubectl logs -f -n kube-system $(kubectl get pod -n kube-system --no-headers -o custom-columns=":metadata.name" | grep "fluentd")
```
Note that fluentd is only configured for the deployed namespace, jacana, and the rest-api pod.

### Watch the logs in kibana

Access Kibana in your browser: http://localhost:5601

* On a first time access of Kibana:
* Click ‘Use my own data’ (the first screen on a first load - may not show either)
* Click the top-left icon (three horizontal bars)
* Select Discover under Kibana
* Click ‘Index Patterns’
* Click ‘Create index pattern’ button
* There should be an existing index starting with the word ‘logstash’. In the ‘Index pattern name’ type ‘logstash’, then click ‘Next step’
* The ‘Time field’ to select is ‘@timestamp’.
* Click ‘Create index pattern’
* Click the top-left icon (three horizontal bars)
* Click Discover under Kibana

You should now see log lines, bar charts, etc.

**NOTE** Kibana and Elastic can be enabled/disabled in `k8s/values.yaml` by editing the property:
```
logging:
  visualization:
    enabled: false
```
This ability was added when it was noticed that running all four services together sometimes ran into performance problems for minikube. Plus it is sometimes nice to run a little 'leaner' if only testing the REST API.

If you do modify the values in `k8s/values.yaml` remember to upgrade helm:
```
helm upgrade jacana-release k8s/. -n jacana
```
And, again, Kibana pod may say 'Running', but it may not be accessible for a few minutes.

### Metrics

By default Helidon exposes a `health` endpoint and a `metrics` endpoint that is compatible with Prometheus (and json).

Get the health of the service
```
curl -s -X GET http://localhost:8080/health | jq
```
Should say the status is UP

Get the metrics of the service in json
```
curl -s -X GET http://localhost:8080/metrics -H 'Accept: application/json' | jq
```

Get the metrics of the service for Prometheus
```
curl -s -X GET http://localhost:8080/metrics 
```
This example project does not have a Prometheus deployment scraping metrics. 

The jacana rest-api exposes counts and times for the `insert`, `update`, and `delete` REST API calls. As well as counts of any errors invoking those endpoints.

# All test cases

## Insert

Insert a customer. The response code is 200 OK and the body is the customer that was created which should be the same as the POST body EXCEPT with a unique, guid, field included.
**NOTE**: Null fields are not returned. Working on how to fix things to return null fields.
```
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers | jq
```

List all of the customers which will include the customer that was just created
```
curl -s -X GET http://localhost:8080/customers | jq
```

Get the customer by email
```
curl -s -X GET 'http://localhost:8080/customers?email=foo@example.com' | jq
```

Get the customer by guid

**NOTE**: Use the guid from the response in the create command.
```
curl -s -X GET 'http://localhost:8080/customers?guid=d97318b6-074b-4bb9-bdfa-ecab137876d1' | jq
```

Getting a customer with a non-existent email (or guid) will result in a 200 OK, but with an empty array.
The choice of a 200 OK with an empty array is to continue with the interface of listing customers.
```
curl -s -X GET 'http://localhost:8080/customers?email=nothing' | jq
curl -s -X GET 'http://localhost:8080/customers?guid=nothing' | jq
```

Attempting to create a customer with the same email as an existing customer will return a 400 error.
```
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers | jq
```

Attempting to create a customer without an email will return a 400 error.
```
curl -s -X POST -H "Content-Type: application/json" -d '{"phone":"555-555-1212"}' http://localhost:8080/customers | jq
```

## Update

Create two customers
```
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"bar@example.com"}' http://localhost:8080/customers | jq
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"car@example.com"}' http://localhost:8080/customers | jq
```

Update the email address of the second customer

**NOTE**: Use the guid from the response in the second create command.
```
curl -s -X PUT -H "Content-Type: application/json" -d '{"email":"newcar@example.com"}' http://localhost:8080/customers/6761f4ee-bb3e-4a26-83de-90f194866210 | jq
```

Update the email address of the second customer to match the first customer. This will return a 400 error.

**NOTE**: Use the guid from the response in the second create command.
```
curl -s -X PUT -H "Content-Type: application/json" -d '{"email":"bar@example.com"}' http://localhost:8080/customers/6761f4ee-bb3e-4a26-83de-90f194866210 | jq
```

Update a non-existent customer will return a 404 not found.
```
curl -s -X PUT -H "Content-Type: application/json" -d '{"email":"bar@example.com"}' http://localhost:8080/customers/foobar | jq
```

## Delete

Create a customer
```
curl -s -X POST -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers | jq
```

Deleting the customer results in a 200 OK and a body containing the details of the customer that was deleted.

**NOTE**: Use the guid returned from creating the customer.
```
curl -s -X DELETE -H "Content-Type: application/json" http://localhost:8080/customers/1bf94f25-5c18-4572-bb68-556db52918e1 | jq
```

Deleting the customer again results in a 404 as the customer is not found.

**NOTE**: Use the guid returned from creating the customer.
```
curl -s -X DELETE -H "Content-Type: application/json" http://localhost:8080/customers/1bf94f25-5c18-4572-bb68-556db52918e1 | jq
```

Deleting a customer with a non-existent guid also results in a 404 not found.
```
curl -s -X DELETE -H "Content-Type: application/json" http://localhost:8080/customers/foobar | jq
```

# Updating the code

Build the code. This will run the test cases which are skipped when building the Docker image, so this is an important step for validation.
```
cd rest-api
mvn package
cd ..
```

Build Docker image
```
docker build -t jacana-rest-api rest-api/.
```

Tag and Push the Docker image
```
docker tag jacana-rest-api localhost:5200/jacana-rest-api
docker push localhost:5200/jacana-rest-api
```

Delete the jacana-rest-api pod so that it is recreated with the actual latest image
```
kubectl delete pod $(kubectl get pod -n jacana --no-headers -o custom-columns=":metadata.name" | grep "jacana-rest-api") -n jacana
```
List the pods to see when they are running again
```
kubectl get pod -n jacana
```

Test the REST API
```
curl -s -X GET http://localhost:8080/customers | jq
```



