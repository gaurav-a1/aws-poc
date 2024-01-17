package com.serverless.helper;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.Constants;
import com.serverless.request.FetchImageRequest;
import com.serverless.request.UploadImageRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDB {
    private AmazonDynamoDB amazonDynamoDB;
    private final Regions REGION = Regions.AP_SOUTH_1;


    public void initDynamoDB(LambdaLogger logger) {
        this.amazonDynamoDB = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(REGION)
                .build();
    }

    public List<Map<String, AttributeValue>> getImageList(FetchImageRequest fetchImageRequest, LambdaLogger logger) {

        String filterExpression = "createdAt >= :value1 AND createdAt <= :value2";
        Map<String, AttributeValue> expressionAttributeValuesMap = new HashMap<>();
        expressionAttributeValuesMap.put(":value1", new AttributeValue(fetchImageRequest.getStartDate()));
        expressionAttributeValuesMap.put(":value2", new AttributeValue(fetchImageRequest.getEndDate()));
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(Constants.DYNAMO_DB_TABLE)
                .withFilterExpression(filterExpression)
                .withExpressionAttributeValues(expressionAttributeValuesMap);
        ScanResult scanResult = this.amazonDynamoDB.scan(scanRequest);
        return scanResult.getItems();
    }

    public void persistData(UploadImageRequest request, LambdaLogger logger, String bucketName) throws Exception{
        Map<String, AttributeValue> record = new HashMap<>();
        ZonedDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).atZone(ZoneId.of("Asia/Kolkata"));
        String nowString = now.toString().split("\\+")[0];
        /*logger.log("now :: " + now);
        logger.log("nowString :: " + nowString);*/
        String key = request.getUserName() + "_" + nowString;
        record.put("id", new AttributeValue(key));
        Map<String, String> imageMetaData = new HashMap<>();
        request.getImageDataList().forEach(imageData -> {
            imageMetaData.put(imageData.getImageName(), bucketName + "/" + request.getUserName()+"/"+imageData.getImageName());
        });
        try {
            record.put("fileList", new AttributeValue(new ObjectMapper().writeValueAsString(imageMetaData)));
        } catch (JsonProcessingException e) {
            logger.log("\npersistData :: error :: " + e);
            throw e;
        }
        record.put("createdAt", new AttributeValue(nowString));

        amazonDynamoDB.putItem(Constants.DYNAMO_DB_TABLE, record);
        amazonDynamoDB.shutdown();
    }
}
