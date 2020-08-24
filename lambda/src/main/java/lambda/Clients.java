package lambda;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;

public class Clients {
    private static final String REGION = System.getenv("REGION");
    private static final AmazonSimpleEmailService simpleEmailService =
            AmazonSimpleEmailServiceClientBuilder.standard().withRegion(REGION).build();

    public AmazonSimpleEmailService getSimpleEmailService() {
        return simpleEmailService;
    }
}
