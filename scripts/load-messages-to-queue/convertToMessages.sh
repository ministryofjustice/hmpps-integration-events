#!/bin/bash
##
## Converts the results of an SQL query containing tab delimited file
#     (containing event-id, HMPPS ID, the last updated datetime and prison ID (optional) )
##    (Prison ID will be `TRN` by default if not provided; `TRN` Prison ID meant "Transfer")
## into an integration event capable of being loaded into an hmpps-integration-api event queue
## e.g
##
## 1502011403      A123456 2017-03-15T15:47:51.000000
## 1502011452      A123457 2025-03-15T13:27:03.000000 MDI
##
## Usage:
## /convertToMessages.sh -t <EVENT_TYPE> -e <ENV> -f <INPUT>
## /convertToMessages.sh -t <EVENT_TYPE> -e <ENV> -u "<URL-SUFFIX>" -f <INPUT>
## e.g ./convertToMessages.sh -e dev -t PERSON_STATUS_CHANGED -f sqlResults.txt > eventMessages.txt
## returns
##
## {"Type":"Notification","MessageId":"...","TopicArn":"...","Message":"{\"eventId\":166,\"hmppsId\":\"A123456\",\"eventType\":\"PERSON_STATUS_CHANGED\",\"prisonId\":\"TRN\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/A123456\",\"lastModifiedDateTime\":\"2017-03-15T15:47:51.000000\"}","Timestamp":"2025-09-24T09:39:24.000Z","SignatureVersion":"1","Signature":"...","SigningCertURL":"...","UnsubscribeURL":"...","MessageAttributes":{"prisonId":{"Type":"String","Value":"TRN"},"eventType":{"Type":"String","Value":"PERSON_STATUS_CHANGED"}}}
## {"Type":"Notification","MessageId":"...","TopicArn":"...","Message":"{\"eventId\":166,\"hmppsId\":\"A123457\",\"eventType\":\"PERSON_STATUS_CHANGED\",\"prisonId\":\"MDI\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/A123457\",\"lastModifiedDateTime\":\"2025-03-15T13:27:03.000000\"}","Timestamp":"2025-09-24T09:39:24.000Z","SignatureVersion":"1","Signature":"...","SigningCertURL":"...","UnsubscribeURL":"...","MessageAttributes":{"prisonId":{"Type":"String","Value":"MDI"},"eventType":{"Type":"String","Value":"PERSON_STATUS_CHANGED"}}}
##
## e.g. ./convertToMessages.sh -e dev -t PRISONER_BASE_LOCATION_CHANGED -u "/prisoner-base-location" -f input-data-dev.txt > eventMessages-dev.txt
## returns
##
## {"Type":"Notification","MessageId":"...","TopicArn":"...","Message":"{\"eventId\":166,\"hmppsId\":\"A123456\",\"eventType\":\"PRISONER_BASE_LOCATION_CHANGED\",\"prisonId\":\"TRN\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/A123456/prisoner-base-location\",\"lastModifiedDateTime\":\"2017-03-15T15:47:51.000000\"}","Timestamp":"2025-10-09T11:17:35.000Z","SignatureVersion":"1","Signature":"...","SigningCertURL":"...","UnsubscribeURL":"...","MessageAttributes":{"prisonId":{"Type":"String","Value":"TRN"},"eventType":{"Type":"String","Value":"PRISONER_BASE_LOCATION_CHANGED"}}}
## {"Type":"Notification","MessageId":"...","TopicArn":"...","Message":"{\"eventId\":166,\"hmppsId\":\"A123457\",\"eventType\":\"PRISONER_BASE_LOCATION_CHANGED\",\"prisonId\":\"MDI\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/A123457/prisoner-base-location\",\"lastModifiedDateTime\":\"2025-03-15T13:27:03.000000\"}","Timestamp":"2025-10-09T11:17:35.000Z","SignatureVersion":"1","Signature":"...","SigningCertURL":"...","UnsubscribeURL":"...","MessageAttributes":{"prisonId":{"Type":"String","Value":"MDI"},"eventType":{"Type":"String","Value":"PRISONER_BASE_LOCATION_CHANGED"}}}

helpFunction()
{
   echo ""
   echo "Usage: $0 -t eventType -e environment -f fileName"
   echo -e "\t-t Provide an eventType"
   echo -e "\t-e Provide an environment"
   echo -e "\t-f Provide a file name containing the file you want to convert"
   echo -e "\t-u Provide an url suffix for the API endpoint after 'v1/persons/{hmppsId}'; Empty by default "
   exit 1 # Exit script after printing help
}

URL_SUFFIX=""
while getopts "t:e:f:u:" opt
do
   case "$opt" in
      t ) EVENT_TYPE="$OPTARG" ;;
      e ) ENV="$OPTARG" ;;
      f ) INPUT="$OPTARG" ;;
      u ) URL_SUFFIX="$OPTARG" ;;
      ? ) helpFunction ;; # Print helpFunction in case parameter is non-existent
   esac
done


if [ -z "$ENV" ]
then
    echo "environment not specified, please specify an environment: (dev/preprod/prod)";
    helpFunction
fi

if ! [[ "$ENV" =~ ^(dev|preprod|prod)$ ]]
then
    echo "Environment is not valid (dev/preprod/prod)";
    helpFunction
fi
if [[ "$ENV" == prod ]]
then
  URL_PREFIX=""
else
  URL_PREFIX="$ENV."
fi

if [ -z "$EVENT_TYPE" ]
then
    echo "Event type not specified, please specify an event type: e.g PERSON_STATUS_CHANGED";
    helpFunction
fi

if [ -z "$INPUT" ]
then
    echo "No file to convert has been specified";
    helpFunction
fi

if ! [ -f "$INPUT" ]
then
    echo "File $INPUT cannot be found";
    helpFunction
fi

while IFS= read -r line
do
  ARR=($line)
  EVENT_ID=166 #Arbitary - or could be ${ARR[0]}
  HMPPS_ID=${ARR[1]}
  LAST_UPDATED=${ARR[2]}
  PRISON_ID=${ARR[3]:-"TRN"}
  TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%S.000Z')
  date=$(echo $(($(date +'%s * 1000 + %-N / 1000000'))))
  URL="https://${URL_PREFIX}integration-api.hmpps.service.justice.gov.uk/v1/persons/$HMPPS_ID$URL_SUFFIX"
  echo "{\"Type\":\"Notification\",\"MessageId\":\"...\",\"TopicArn\":\"...\",\"Message\":\"{\\\"eventId\\\":$EVENT_ID,\\\"hmppsId\\\":\\\"$HMPPS_ID\\\",\\\"eventType\\\":\\\"$EVENT_TYPE\\\",\\\"prisonId\\\":\\\"$PRISON_ID\\\",\\\"url\\\":\\\"$URL\\\",\\\"lastModifiedDateTime\\\":\\\"$LAST_UPDATED\\\"}\",\"Timestamp\":\"$TIMESTAMP\",\"SignatureVersion\":\"1\",\"Signature\":\"...\",\"SigningCertURL\":\"...\",\"UnsubscribeURL\":\"...\",\"MessageAttributes\":{\"prisonId\":{\"Type\":\"String\",\"Value\":\"$PRISON_ID\"},\"eventType\":{\"Type\":\"String\",\"Value\":\"$EVENT_TYPE\"}}}"

done < "$INPUT"
