serve:
	docker-compose up --build -d

unit-test:
	./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	./gradlew check

deploy_local:
	cd terraform
	terraform init
#    terraform plan
#    terraform apply --auto-approve
