package com.serverless.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.serverless.ApiGatewayResponse;
import com.serverless.Handler;
import com.serverless.helper.DynamoDB;
import com.serverless.request.ImageData;
import com.serverless.request.UploadImageRequest;
import com.serverless.response.UploadImageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class S3UploadTriggerHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private static final Logger LOG = LogManager.getLogger(Handler.class);
    private final DynamoDB dynamoDBHelper = new DynamoDB();
    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        LinkedHashMap s3 = (LinkedHashMap)((LinkedHashMap)((List) input.get("Records")).get(0)).get("s3");
        LinkedHashMap bucket = (LinkedHashMap) s3.get("bucket");
        LinkedHashMap object = (LinkedHashMap) s3.get("object");
        String bucketName = (String) bucket.get("name");
        String[] key = ((String) object.get("key")).split("/");
        String userName = key[0];
        String fileName = key[1];
        dynamoDBHelper.initDynamoDB(logger);
        ImageData imageData = ImageData.builder().imageName(fileName).build();
        UploadImageRequest uploadImageRequest = UploadImageRequest.builder()
                                    .userName(userName)
                                    .imageDataList(List.of(imageData))
                                    .build();
        try {
            dynamoDBHelper.persistData(uploadImageRequest, logger, bucketName);
            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(UploadImageResponse.builder().message("success").build())
                    .build();
        } catch (Exception e) {
            LOG.info("Error {} ", e.getMessage());
            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(UploadImageResponse.builder().message("failure").build())
                    .build();
        }
    }
}
