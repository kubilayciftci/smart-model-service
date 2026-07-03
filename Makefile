.PHONY: run build build-native test up down deploy undeploy demo

run:
	./mvnw quarkus:dev

build:
	./mvnw clean package

build-native:
	./mvnw clean package -Dnative -Dquarkus.native.container-build=true

test:
	./mvnw test

up:
	docker compose up -d --build

down:
	docker compose down -v

deploy:
	eval $$(minikube docker-env) && ./mvnw clean package -DskipTests -Dquarkus.container-image.build=true
	kubectl apply -f deploy/k8s/postgres.yaml
	kubectl apply -f target/kubernetes/minikube.yml

undeploy:
	kubectl delete -f target/kubernetes/minikube.yml --ignore-not-found
	kubectl delete -f deploy/k8s/postgres.yaml --ignore-not-found

demo:
	./scripts/demo.sh
