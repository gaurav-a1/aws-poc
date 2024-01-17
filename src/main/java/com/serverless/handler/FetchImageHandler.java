package com.serverless.handler;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.ApiGatewayResponse;
import com.serverless.helper.DynamoDB;
import com.serverless.helper.S3FetchImage;
import com.serverless.request.FetchImageRequest;
import com.serverless.response.FetchImageResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FetchImageHandler  implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final DynamoDB dynamoDBHelper = new DynamoDB();
    private final S3FetchImage fetchImageHelper = new S3FetchImage();
    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> request, Context context) {
        LambdaLogger logger = context.getLogger();
        String body = (String)request.get("body");
        dynamoDBHelper.initDynamoDB(logger);
        FetchImageRequest fetchImageRequest;
        try {
            fetchImageRequest = new ObjectMapper().readValue(body, FetchImageRequest.class);
        } catch (JsonProcessingException e) {
            logger.log("\nerror :: " + e.getMessage() + "\n");
            return ApiGatewayResponse.builder()
                    .setStatusCode(400)
                    .setObjectBody(FetchImageResponse.builder().message("failure :: " + e.getMessage()).build())
                    .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                    .build();
        }
        List<Map<String, AttributeValue>> records = dynamoDBHelper.getImageList(fetchImageRequest, logger);
        if(records == null) {

            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(FetchImageResponse.builder().message("failure, no record").build())
                    .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                    .build();
        } else {
            try {
                return ApiGatewayResponse.builder()
                        .setStatusCode(200)
                        .setObjectBody(FetchImageResponse.builder().message("success").files(getFile(records, logger)).build())
                        .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                        .build();
            } catch (Exception e) {
                return ApiGatewayResponse.builder()
                        .setStatusCode(500)
                        .setObjectBody(FetchImageResponse.builder().message("failure :: " +e.getMessage()).build())
                        .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                        .build();
            }
        }
    }

    private List<byte[]> getFile(List<Map<String, AttributeValue>> records, LambdaLogger logger) throws Exception {
        List<String> filePath = new ArrayList<>();
        records.stream()
                .map(r -> {
                    try {
                        return new ObjectMapper().readValue(r.get("fileList").getS(), Map.class);
                    } catch (JsonProcessingException e) {
                        logger.log("\nerror :: " + e.getMessage() + "\n");
                        return null;
                    }
                }).filter(Objects::nonNull)
                .forEach(imd -> filePath.addAll(imd.values()));
        return fetchImageHelper.fetchFilesFromS3(filePath, logger);
    }
}
