package com.serverless.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class S3FetchImage {
    public List<byte[]> fetchFilesFromS3(List<String> filePathList, LambdaLogger logger) {
        List<byte[]> files = new ArrayList<>();
        AmazonS3Client s3Client = (AmazonS3Client) AmazonS3ClientBuilder.standard().build();
        for(String filePath: filePathList) {
            String pArr[] = filePath.split("/");
            String bucketName = pArr[0];
            String folderName = pArr[1];
            String filename = pArr[2];
            String key = folderName + "/" + filename;

            try(S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName, key))) {
                InputStream fileInputStream = s3Object.getObjectContent();
                //files.add(fileInputStream);
                files.add(IOUtils.toByteArray(fileInputStream));
            } catch(AmazonS3Exception | IOException e) {
                logger.log("\nerror :: " + e.getMessage());
            }
        }
        return files;
    }
}
