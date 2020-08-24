package lambda;

public class Values {
    private static final String CORS_ALLOW_ORIGIN = System.getenv("CORS_ALLOW_ORIGIN");
    private static final String DESTINATION_EMAIL = System.getenv("DESTINATION_EMAIL");
    private static final String RECAPTCHA_V2_SECRET_KEY = System.getenv("RECAPTCHA_V2_SECRET_KEY");

    public String getCorsAllowOrigin() {
        return CORS_ALLOW_ORIGIN;
    }

    public String getDestinationEmail() {
        return DESTINATION_EMAIL;
    }

    public String getRecaptchaV2SecretKey() {
        return RECAPTCHA_V2_SECRET_KEY;
    }
}
