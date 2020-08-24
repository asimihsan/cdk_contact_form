package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lambda.exception.UnrecognizedApiResourceException;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHandler implements RequestStreamHandler {
    private static final Logger log = Logger.getLogger(BaseHandler.class);
    private static final Clients defaultClients = new Clients();
    private static final Values defaultValues = new Values();

    private static final int OK_STATUS_CODE = 200;
    private static final int CLIENT_ERROR_STATUS_CODE = 400;
    private static final int SERVER_ERROR_STATUS_CODE = 500;
    private static final String STATUS_CODE = "statusCode";
    private static final String BODY = "body";
    private static final String HEADERS = "headers";

    protected static final Gson gson = new Gson();
    protected final Clients clients;
    protected final Values values;

    public BaseHandler() {
        clients = defaultClients;
        values = defaultValues;
    }

    @SneakyThrows(IOException.class)
    @SuppressWarnings("unchecked")
    private Map<String, Object> inputStreamJsonToMap(final InputStream is) {
        final Map<String, Object> inputMap;
        try (final InputStreamReader isr = new InputStreamReader(is);
             final BufferedReader br = new BufferedReader(isr)) {
            inputMap = (Map<String, Object>)gson.fromJson(br, Map.class);
        }
        return inputMap;
    }

    @SneakyThrows({IOException.class})
    private void writeMapToOutputStream(final Map<String, Object> map, final OutputStream os) {
        final String output = gson.toJson(map);
        try (final OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
             final BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(output);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stringToJsonMap(final String string) {
        return (Map<String, Object>)gson.fromJson(string, Map.class);
    }

    /**
     * Handle the request.
     */
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED",
            justification = "Spotbugs doesn't support @CanIgnoreReturnValue")
    @Override
    public void handleRequest(final InputStream inputStream,
                              final OutputStream outputStream,
                              final Context context) {
        final Map<String, Object> inputMap = inputStreamJsonToMap(inputStream);
        System.out.println("here");
        log.info(String.format("entry. input: %s", inputMap));
        final ImmutableMap.Builder<String, String> outputHeaders = ImmutableMap.<String, String>builder()
                .put("Content-Type", "application/json")
                .put("Access-Control-Allow-Origin", values.getCorsAllowOrigin())
                .put("Access-Control-Allow-Methods", "POST, OPTIONS")
                .put("Access-Control-Allow-Credentials", "false")
                .put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,X-Amz-User-Agent")
                .put("Access-Control-Max-Age", "86400");
        final ImmutableMap.Builder<String, Object> responseBuilder = ImmutableMap.<String, Object>builder()
                .put("isBase64Encoded", false);
        final Map<String, String> inputHeaders = getHeaders(inputMap);
        log.info(String.format("inputHeaders: %s", inputHeaders));
        final String resource = (String) inputMap.get("path");
        final ApiType apiType;
        switch (resource) {
            case "/api/post_contact_form":
                apiType = ApiType.PostContactForm;
                break;

            default:
                final String message = String.format("Unrecognized API resource: %s", resource);
                throw new UnrecognizedApiResourceException(message);
        }

        final Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        String outputBody = "";

        final String inputBody = (String) inputMap.get(BODY);
        final Map<String, Object> input;
        try {
            input = stringToJsonMap(inputBody);
        } catch (final Exception e) {
            log.error("Exception while JSON deserializing input: ", e);
            log.error(String.format("Exception message: %s", e.getMessage()));
            responseBuilder.put(STATUS_CODE, CLIENT_ERROR_STATUS_CODE);
            outputBody = gson.toJson(result);
            finishHandleRequest(outputHeaders, outputBody, responseBuilder, outputStream);
            return;
        }

        try {
            outputBody = handleRequestInternal(apiType, input);
        } catch (final Exception e) {
            log.error("Uncaught exception: ", e);
            log.error(String.format("Exception message: %s", e.getMessage()));
            responseBuilder.put(STATUS_CODE, SERVER_ERROR_STATUS_CODE);
            outputBody = gson.toJson(result);
            finishHandleRequest(outputHeaders, outputBody, responseBuilder, outputStream);
            return;
        }

        responseBuilder.put(STATUS_CODE, OK_STATUS_CODE);
        finishHandleRequest(outputHeaders, outputBody, responseBuilder, outputStream);
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED",
            justification = "Spotbugs doesn't support @CanIgnoreReturnValue")
    private void finishHandleRequest(
            final ImmutableMap.Builder<String, String> outputHeaders,
            final String body,
            final ImmutableMap.Builder<String, Object> responseBuilder,
            final OutputStream output) {
        responseBuilder.put(HEADERS, outputHeaders.build());
        responseBuilder.put(BODY, body);
        final Map<String, Object> responseMap = responseBuilder.build();
        log.info(String.format("Returning response: %s", responseMap));
        writeMapToOutputStream(responseMap, output);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getHeaders(final Map<String, Object> inputMap) {
        return (Map<String, String>) inputMap.get("headers");
    }

    public abstract String handleRequestInternal(final ApiType apiType, final Map<String, Object> input);
}