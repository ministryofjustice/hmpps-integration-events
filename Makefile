-include .env

create-env-file:
	./scripts/create-env-file.sh

check-required-vars:
	@echo "Checking for required environment variables..."
	@if [ -z "${DB_USER}" ] || [ -z "${DB_PASS}" ] || [ -z "${API_KEY}" ]; then \
		echo "Error: Required variables (DB_USER, DB_PASS, API_KEY) are not set."; \
		echo "Please ensure they are defined in your .env file or environment. You can run make create-env-file to generate the .env file locally."; \
		exit 1; \
	fi
	@echo "All required variables are set."

serve: check-required-vars
	docker-compose up --build -d --wait

unit-test: check-required-vars
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} ./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check: check-required-vars
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} ./gradlew check

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info