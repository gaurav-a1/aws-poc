## Introduction
This application is a POC for uploading/downloading images to/from S3 Bucket, with making use of AWS Lambda Function and API Gateway.

### Pre-Requisites
An AWS account/profile with adequate privileges to create Lambda Function, S3 Bucket, DynamoDB table, API Gateway endpoints and resources, and to create roles/permission to grant access to these services to connect to each other.  

### Configuration Required
1. In serverless.yml, provide values for provider.profile (the same profile configured as part of pre-requisite step.). Change the region as per requirement. 
2. In serverless.yml, provide values for custom.tableName and custom.bucketName. 
3. Same values to be given in com.serverless.Constants for DYNAMO_DB_TABLE and BUCKET_NAME.

### Deploy/Remove
1. Deploy -- sls deploy. Take note of API end points for getImage and postImage methods. Update the same end point in Angular App.
2. Remove -- sls remove