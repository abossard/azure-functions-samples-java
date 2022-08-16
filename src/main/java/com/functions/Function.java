/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.functions;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.ApplicationCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

        final GraphServiceClient graphClient = GraphServiceClient
            .builder()
            .authenticationProvider(new TokenCredentialAuthProvider(credential))
            .buildClient();
        ApplicationCollectionPage result = graphClient.applications().buildRequest().get();

        // Parse query parameter

        return request.createResponseBuilder(HttpStatus.OK).body(result.getCount()).build();
    }

    public static int count = 1;

    /**
     * This function listens at endpoint "/api/HttpExampleRetry". The function is re-executed in case of errors until the maximum number of retries occur.
     * Retry policies: https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-error-pages?tabs=java
     */
    @FunctionName("HttpExampleRetry")
    @FixedDelayRetry(maxRetryCount = 3, delayInterval = "00:00:05")
    public HttpResponseMessage HttpExampleRetry(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) throws Exception {
        context.getLogger().info("Java HTTP trigger processed a request.");

        if(count<3) {
            count ++;
            throw new Exception("error");
        }
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

        final GraphServiceClient graphClient = GraphServiceClient
            .builder()
            .authenticationProvider(new TokenCredentialAuthProvider(credential))
            .buildClient();
        ApplicationCollectionPage result = graphClient.applications().buildRequest().get();

        // Parse query parameter

        return request.createResponseBuilder(HttpStatus.OK).body(result.getCurrentPage().stream().count()).build();

    }

    /**
     * This function listens at endpoint "/api/HttpTriggerJavaVersion".
     * It can be used to verify the Java home and java version currently in use in your Azure function
     */
    @FunctionName("HttpTriggerJavaVersion")
    public static HttpResponseMessage HttpTriggerJavaVersion(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context
    ) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        final String javaVersion = getJavaVersion();
        context.getLogger().info("Function - HttpTriggerJavaVersion" + javaVersion);
        return request.createResponseBuilder(HttpStatus.OK).body("HttpTriggerJavaVersion").build();
    }

    public static String getJavaVersion() {
        return String.join(" - ", System.getProperty("java.home"), System.getProperty("java.version"));
    }
}
