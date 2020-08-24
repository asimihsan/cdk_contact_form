package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;
import java.util.List;

public class CdkContactFormApp {
    public static void main(final String[] args) {
        final App app = new App();

        final Environment environment = Environment.builder()
                .account("519160639284")
                .region("us-east-2")
                .build();

        final String contactEmailSecretArn = "arn:aws:secretsmanager:us-east-2:519160639284:secret" +
                ":CONTACT_FORM_EMAIL_ADDRESS-wiqChV";
        final String recaptchaV2SecretArn = "arn:aws:secretsmanager:us-east-2:519160639284:secret" +
                ":ASIM_IHSAN_IO_RECAPTCHA_V2_SECRET_KEY-FY7ilU";
        final String sesEmailArnSecretArn = "arn:aws:secretsmanager:us-east-2:519160639284:secret" +
                ":ASIM_IHSAN_IO_SES_EMAIL_ARN-FZsOoj";

        // --------------------------------------------------------------------
        //  Pre-prod.
        // --------------------------------------------------------------------
        final String preprodStackName = "preprod-CdkContactFormStack";
        final String preprodDomainName = "preprod-contact.ihsan.io";
        final List<String> preprodCorsAllowOrigin = Arrays.asList(
                "*"
//                "http://192.168.1.17:5000",
//                "https://preprod-asim.ihsan.io",
//                "https://preprod-contact.ihsan.io"
        );
        final Stack preprodStack = new CdkContactFormStack(app, preprodStackName,
                preprodDomainName,
                preprodCorsAllowOrigin,
                contactEmailSecretArn,
                sesEmailArnSecretArn,
                recaptchaV2SecretArn,
                StackProps.builder()
                        .env(environment)
                        .description("Pre-prod contact form stack")
                        .build());

        // --------------------------------------------------------------------
        //  Prod.
        // --------------------------------------------------------------------
        final String prodStackName = "prod-CdkContactFormStack";
        final String prodDomainName = "contact.ihsan.io";
        final List<String> prodCorsAllowOrigin = Arrays.asList(
                "*"
//                "https://asim.ihsan.io",
//                "https://contact.ihsan.io"
        );
        final Stack prodStack = new CdkContactFormStack(app, prodStackName,
                prodDomainName,
                prodCorsAllowOrigin,
                contactEmailSecretArn,
                sesEmailArnSecretArn,
                recaptchaV2SecretArn,
                StackProps.builder()
                        .env(environment)
                        .description("Prod contact form stack")
                        .build());
        // --------------------------------------------------------------------

        app.synth();
    }
}
