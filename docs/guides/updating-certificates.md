# Updating Certificates

This API requires access to the HMPPS Integration API to update its consumer config. You access the API via mTLS. If the certificates expire for the mTLS, 

1. Generate some new certificates using the `generate.sh` [script in the Integration API](https://github.com/ministryofjustice/hmpps-integration-api/tree/main/scripts/client_certificates/generate.sh).
2. Upload the new certificates as a p12 using the `uploadP12.sh` [script in the Integration API](https://github.com/ministryofjustice/hmpps-integration-api/tree/main/scripts/client_certificates/uploadP12.sh).