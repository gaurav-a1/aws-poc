package com.serverless.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.ApiGatewayResponse;
import com.serverless.Constants;
import com.serverless.Handler;
import com.serverless.request.UploadImageRequest;
import com.serverless.response.UploadImageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;


public class UploadImageHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    private static final Logger LOG = LogManager.getLogger(Handler.class);

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
    //private final DynamoDB dynamoDBHelper = new DynamoDB();
    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> request, Context context) {
        LambdaLogger logger = context.getLogger();
        String body = (String)request.get("body");
        UploadImageRequest uploadImageRequest;
        try {
            uploadImageRequest = new ObjectMapper().readValue(body, UploadImageRequest.class);
            String userName = uploadImageRequest.getUserName();
            uploadImageRequest.getImageDataList().forEach(idl -> {
                byte[] decodedImage = Base64.getDecoder().decode(idl.getImageBase64());
                float fileSizeInMB = (float) decodedImage.length /(1024*1024);
                String dataType = idl.getDataType().split("/")[1];
                if((dataType.equals("mp4"))
                        && fileSizeInMB > 10) {
                    throw new RuntimeException(String.format("Video %s size exceeds 10  MB", idl.getImageName()));
                } else if ((dataType.equals("png") || dataType.equals("jpg") || dataType.equals("jpeg"))
                                && fileSizeInMB > 5) {
                    throw new RuntimeException(String.format("Image %s size exceeds 5  MB", idl.getImageName()));
                }
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(decodedImage.length);
                metadata.setContentType(idl.getDataType());
                String imageName = userName + "/" + idl.getImageName();
                s3Client.putObject(Constants.BUCKET_NAME, imageName, new ByteArrayInputStream(decodedImage), metadata);
            });
            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(UploadImageResponse.builder().message("success").build())
                    .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                    .build();
        } catch (Exception e) {
            logger.log("\nerror :: " + e.getMessage() + "\n");
            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(UploadImageResponse.builder().message("Failed")
                            .errorMessage(e.getMessage())
                            .errorStackTrace(Arrays.toString(e.getStackTrace()))
                            .build())
                    .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                    .build();
        }
    }

}
