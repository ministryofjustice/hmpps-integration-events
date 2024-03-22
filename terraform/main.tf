provider "aws" {
  region                      = "ae-west-2"
  access_key                  = "fake"
  secret_key                  = "fake"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    postgres = "http://localhost:4566"
    sqs = "http://localhost:4566"
    sns = "http://localhost:4566"
  }
}

resource "aws_db_instance" "event_store" {
  identifier = "event_store"
  engine = "postgres"
  instance_class = "db.m3.medium"

  username = "postgres"
  password = "alexIsC00l"
}