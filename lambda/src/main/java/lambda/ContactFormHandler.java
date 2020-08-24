package lambda;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import lombok.SneakyThrows;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactFormHandler extends BaseHandler {
    private static final Logger log = Logger.getLogger(ContactFormHandler.class);

    private Content getStringAsContent(final String string) {
        return new Content().withCharset("UTF-8").withData(string);
    }

    private String buildEmail(final String contactName,
                              final String contactEmail,
                              final String contactMessage) {
        final StringBuilder output = new StringBuilder();
        output.append("Name: ").append(contactName);
        output.append("\n\n");
        output.append("Email: ").append(contactEmail);
        output.append("\n\n");
        output.append("Message: ").append(contactMessage);
        return output.toString();
    }


    @SneakyThrows(IOException.class)
    @SuppressWarnings("unchecked")
    private boolean verifyRecaptcha(final String recaptchaResponse) {
        log.info("verifyRecaptcha entry");
        final HttpPost request = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
        final List<NameValuePair> urlParameters = Arrays.asList(
                new BasicNameValuePair("secret", values.getRecaptchaV2SecretKey()),
                new BasicNameValuePair("response", recaptchaResponse)
                //new BasicNameValuePair("remoteip"),
        );
        request.setEntity(new UrlEncodedFormEntity(urlParameters));

        try (final CloseableHttpClient httpClient = HttpClients.createDefault();
             final CloseableHttpResponse response = httpClient.execute(request)) {
            final String result = EntityUtils.toString(response.getEntity());
            log.info(String.format("verifyRecaptcha result: %s", result));
            final Map<String, Object> resultMap = (Map<String, Object>)gson.fromJson(result, Map.class);
            final boolean success = (boolean)resultMap.get("success");
            log.info(String.format("verifyRecaptcha exit, returning: %s", success));
            return success;
        }
    }

    @Override
    public String handleRequestInternal(ApiType apiType, final Map<String, Object> input) {
        log.info(String.format("input: %s", input));

        final Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        final String recaptchaResponse = (String)input.get("g-recaptcha-response");
        final boolean isRecaptchaVerified = verifyRecaptcha(recaptchaResponse);
        if (!isRecaptchaVerified) {
            log.info("recaptcha did not verify");
            return gson.toJson(result);
        }

        final String sourceEmail = values.getDestinationEmail();
        final String destinationEmail = values.getDestinationEmail();
        final String contactName = (String)input.get("name");
        final String contactEmail = (String)input.get("email1");
        final String contactMessage = (String)input.get("message");

        final SendEmailRequest sendEmailRequest = new SendEmailRequest()
                .withSource(sourceEmail)
                .withDestination(new Destination().withToAddresses(destinationEmail))
                .withMessage(new Message()
                        .withSubject(getStringAsContent("Contact from personal blog"))
                        .withBody(
                                new Body().withText(getStringAsContent(
                                        buildEmail(contactName, contactEmail, contactMessage))
                        ))
                );
        try {
            clients.getSimpleEmailService().sendEmail(sendEmailRequest);
            log.info("Email sent!");
        } catch (final Exception e) {
            log.error("Failed to send email!", e);
            return gson.toJson(result);
        }

        result.put("success", true);
        return gson.toJson(result);
    }

}
