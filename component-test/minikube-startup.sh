#!/bin/bash

delete_all_minikube_resources() {
   kubectl --context=minikube delete --all deployments
   kubectl --context=minikube delete --all services
}

create_deployment_and_service() {
    echo "Deploying ${1} pod"
    kubectl apply -f "$deploymentsDirectory/${1}-deployment.yml"

    echo "Deploying service for ${1}"
    kubectl apply -f "$servicesDirectory/${1}-service.yml"
}

start_minikube() {
    if [[ $(minikube status | grep -o "Running" 2>/dev/null | uniq) != "Running" ]]; then
        minikube start --cpus 2 --memory 8192 --kubernetes-version v1.5.3
        # Wait for cluster to start
        echo "Waiting for cluster to start"
        while [ "$ready" != "Running" ]; do
            ready=$(minikube status | grep localkube | grep -o "Running")
            sleep 10
        done
    fi
}

setup_topic() {
    echo "Setting up kafka topic - started"
    pod_name=$(kubectl get pods | awk '/kafka/{print$1}')

    echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --create --topic movie-message --partitions 1 --replication-factor 1" | kubectl exec "$pod_name" -it bash
    echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --create --topic film-ratings-message --partitions 1 --replication-factor 1" | kubectl exec "$pod_name" -it bash
    echo "==========================================================="
    echo "List of available topics"
    echo "==========================================================="
    echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --list" | kubectl exec "$pod_name" -it bash
    echo "Setting up kafka topic - completed"
}

echo "==========================================================="
echo "Minikube setup started..."
echo "==========================================================="
# Switch context to minikube
echo "Switching context to minikube"
kubectl config use-context minikube

# Kubernetes Variables
kubernetesDirectory="kubernetes"
servicesDirectory="$kubernetesDirectory/services"
deploymentsDirectory="$kubernetesDirectory/deployments"
initial_deployments=$( printf "zookeeper" | sort )
initial_deployments_names=$( printf "zookeeper" )

echo "${1}"

# Start minikube if not already running
start_minikube

# Clean up minikube before starting the tests
echo "Deleting all existing minikube resources"
delete_all_minikube_resources

# Pod / service deployments
for i in $(printf "$initial_deployments"); do
	create_deployment_and_service "${i}"
done

echo "Waiting for initial deployments to be ready..."
while [ "$ready" != "$initial_deployments_names" ]; do
	sleep 5
	ready=$(kubectl get deployments | awk '{if ($5==1) print $1 }' | sort)
	printf "%0.s-" {1..1}
done

echo "Setup of initial deployments - complete"

# Set up kafka
create_deployment_and_service "kafka"

echo "Waiting for kafka to be ready..."
while [ "$ready" != "started (kafka.server.KafkaServer)" ]; do
	sleep 5
	ready=$(kubectl logs `kubectl get pods | awk '/kafka/{print$1}'` | grep -o "started (kafka.server.KafkaServer)")
	printf "%0.s-" {1..1}
done

# Set up Kafka Topic
echo "Setting up kafka topic..."
setup_topic

# Set docker env
echo "Setting docker env..."
eval $(minikube docker-env)

# Open minikube dashboard
echo "Opening minikube dashboard..."
minikube dashboard &

echo "==========================================================="
echo "Minikube setup completed."
echo "==========================================================="

exit 0
