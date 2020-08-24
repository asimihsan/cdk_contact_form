package com.myorg;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.Cors;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaIntegrationOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cloudfront.Behavior;
import software.amazon.awscdk.services.cloudfront.CloudFrontAllowedMethods;
import software.amazon.awscdk.services.cloudfront.CloudFrontWebDistribution;
import software.amazon.awscdk.services.cloudfront.CloudFrontWebDistributionProps;
import software.amazon.awscdk.services.cloudfront.CustomOriginConfig;
import software.amazon.awscdk.services.cloudfront.PriceClass;
import software.amazon.awscdk.services.cloudfront.SSLMethod;
import software.amazon.awscdk.services.cloudfront.SecurityPolicyProtocol;
import software.amazon.awscdk.services.cloudfront.SourceConfiguration;
import software.amazon.awscdk.services.cloudfront.ViewerCertificate;
import software.amazon.awscdk.services.cloudfront.ViewerCertificateOptions;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.iam.IPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdkContactFormStack extends Stack {
    public CdkContactFormStack(final Construct scope,
                               final String id,
                               final String domainName,
                               final List<String> corsAllowOrigin,
                               final String contactEmailSecretArn,
                               final String sesEmailArnSecretArn,
                               final String recaptchaV2SecretArn,
                               final StackProps props) {
        super(scope, id, props);

        final ISecret destinationEmail = Secret.fromSecretAttributes(this, "DestinationEmail",
            SecretAttributes.builder()
                    .secretArn(contactEmailSecretArn)
                    .build());
        final ISecret sesEmailArn = Secret.fromSecretAttributes(this, "SesEmailArn",
                SecretAttributes.builder()
                        .secretArn(sesEmailArnSecretArn)
                        .build());
        final ISecret recaptchaV2SecretKey = Secret.fromSecretAttributes(this, "RecaptchaV2SecretKey",
                SecretAttributes.builder()
                        .secretArn(recaptchaV2SecretArn)
                        .build());

        // --------------------------------------------------------------------
        //  Lambda.
        // --------------------------------------------------------------------
        final Map<String, String> environment = new HashMap<>();
        environment.put("CORS_ALLOW_ORIGIN", Joiner.on(", ").join(corsAllowOrigin));
        environment.put("DESTINATION_EMAIL", destinationEmail.getSecretValue().toString());
        environment.put("RECAPTCHA_V2_SECRET_KEY", recaptchaV2SecretKey.getSecretValue().toString());
        environment.put("REGION", props.getEnv().getRegion());
        final Function lambda = Function.Builder.create(this, "Lambda")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../lambda/build/libs/lambda.jar"))
                .handler("lambda.ContactFormHandler::handleRequest")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .environment(environment)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .build();

        final IPolicy lambdaCanSendEmailPolicy =
                Policy.Builder.create(this, "LambdaCanSendEmailPolicy")
                        .roles(Collections.singletonList(lambda.getRole()))
                        .statements(Collections.singletonList(
                                PolicyStatement.Builder.create()
                                        .resources(Collections.singletonList(
                                                sesEmailArn.getSecretValue().toString()))
                                        .actions(Collections.singletonList("ses:SendEmail"))
                                        .build()
                        ))
                        .build();
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        //  API gateway.
        // --------------------------------------------------------------------
        final RestApi api = RestApi.Builder.create(this, "ContactApi")
                .endpointConfiguration(EndpointConfiguration.builder()
                        .types(Collections.singletonList(EndpointType.REGIONAL))
                        .build())
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowOrigins(corsAllowOrigin)
                        .allowCredentials(false)
                        .allowHeaders(Cors.DEFAULT_HEADERS)
                        .allowMethods(ImmutableList.of("POST", "OPTIONS"))
                        .maxAge(Duration.seconds(86400))
                        .build())
                .deployOptions(StageOptions.builder()
                        .tracingEnabled(true)
                        .build())
                .build();
        final Resource rootResource = api.getRoot().addResource("api");

        final Resource getResource = rootResource.addResource("post_contact_form");
        final LambdaIntegration postContactFormIntegration = new LambdaIntegration(lambda,
                LambdaIntegrationOptions.builder().proxy(true).build());
        getResource.addMethod("POST", postContactFormIntegration);
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        //  Create the certificate that CloudFront will use.
        // --------------------------------------------------------------------
        final IHostedZone hostedZone = HostedZone.fromLookup(this, "HostedZone",
                HostedZoneProviderProps.builder()
                        .domainName("ihsan.io")
                        .privateZone(false)
                        .build());
        final ICertificate certificate = DnsValidatedCertificate.Builder.create(
                this,
                "Certificate"
        )
                .domainName(domainName)

                // CloudFront requires ACM certificates be in us-east-1
                .region("us-east-1")

                .hostedZone(hostedZone)
                .build();

        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        //  CloudFront distribution for static assets and backend.
        // --------------------------------------------------------------------
        final String apiGatewayDomainName = String.format("%s.execute-api.%s.amazonaws.com",
                api.getRestApiId(),
                props.getEnv().getRegion());
        final List<SourceConfiguration> sourceConfigurations = Collections.singletonList(
                SourceConfiguration.builder()
                        .customOriginSource(CustomOriginConfig.builder()
                                .originPath("/prod")
                                .domainName(apiGatewayDomainName)
                                .originKeepaliveTimeout(Duration.seconds(60))
                                .build())
                        .behaviors(Collections.singletonList(Behavior.builder()
                                .isDefaultBehavior(true)
                                .pathPattern("/api/*")
                                .allowedMethods(CloudFrontAllowedMethods.ALL)
                                .build()))
                        .build()
        );
        final CloudFrontWebDistribution distribution = new CloudFrontWebDistribution(this, "CloudFront",
                CloudFrontWebDistributionProps.builder()
                        .originConfigs(sourceConfigurations)
                        .viewerCertificate(ViewerCertificate.fromAcmCertificate(certificate,
                                ViewerCertificateOptions.builder()
                                        .aliases(Collections.singletonList(domainName))
                                        .securityPolicy(SecurityPolicyProtocol.TLS_V1_2_2018)
                                        .sslMethod(SSLMethod.SNI)
                                        .build()))
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .priceClass(PriceClass.PRICE_CLASS_200)
                        .defaultRootObject("")
                        .build());
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        //  The CloudFront distribution is using the domain as a CNAME, but you
        //  need the DNS A record from the domain name to CloudFront.
        // --------------------------------------------------------------------
        ARecord.Builder.create(this, "DnsAlias")
                .zone(hostedZone)
                .recordName(domainName + ".")
                .target(RecordTarget.fromAlias(new CloudFrontTarget(distribution)))
                .build();
        // --------------------------------------------------------------------
    }
}
