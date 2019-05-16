#!/usr/bin/env bash
PARAMS=params.properties
source $PARAMS
export AWS_PROFILE
export AWS_DEFAULT_REGION

./mvnw package -f FeedScanner &&
sam package --template-file template.yaml --output-template-file compiled.yaml --s3-bucket $BUCKET
sam deploy --template-file ./compiled.yaml --capabilities CAPABILITY_IAM --stack-name DiscordElasticFeed --parameter-overrides $(cat $PARAMS)
