package com.serverless.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadImageResponse {
    private String message;
    private String userName;
    private String errorMessage;
    private String errorStackTrace;
}
