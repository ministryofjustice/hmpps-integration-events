-include .env

create-env-file:
	./scripts/create-env-file.sh

serve:
	docker-compose up --build -d --wait

unit-test:
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} ./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} ./gradlew check

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info