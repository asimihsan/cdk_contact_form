-injars         build/libs/lambda.jar
-outjars        build/libs/lambda_proguard.jar
-libraryjars    <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-libraryjars    <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)
-libraryjars    <java.home>/jmods/java.logging.jmod(!**.jar;!module-info.class)
-libraryjars    <java.home>/jmods/java.management.jmod(!**.jar;!module-info.class)
-libraryjars    <java.home>/jmods/java.sql.jmod(!**.jar;!module-info.class)
-libraryjars    <java.home>/jmods/java.xml.jmod(!**.jar;!module-info.class)

# Keep filenames and line numbers
-keepattributes SourceFile, LineNumberTable

-optimizationpasses 5

# Disable certain proguard optimizations which remove stackframes
-optimizations !method/inlining/*

-dontobfuscate
-optimizations !code/allocation/variable

-keep class lambda.** {*; }

-keep class com.amazonaws.* {*; }
-keep class com.amazonaws.ClientConfiguration {*; }
-keep class com.amazonaws.auth.** {*; }
-keep class com.amazonaws.internal.config.** {*; }
-keep class com.amazonaws.metrics.internal.cloudwatch.DefaultMetricCollectorFactory {*; }
-keep class com.amazonaws.partitions.** {*; }
-keep class com.amazonaws.services.lambda.runtime.** {*; }
-keep class com.amazonaws.services.securitytoken.internal.STSProfileCredentialsService {*; }
-keep class com.amazonaws.xray.** {*; }
-keep class com.fasterxml.jackson.core.type.TypeReference {*; }
-keep class com.fasterxml.jackson.databind.ser.std.StdJdkSerializers {*; }
-keep class com.google.common.collect.ImmutableMap$Builder {*; }
-keep class org.apache.commons.logging.impl.Log4JLogger {*; }
-keep class org.apache.commons.logging.impl.LogFactoryImpl {*; }
-keep class org.apache.commons.text.translate.** {*; }
-keep class org.apache.log4j.** {*; }
-keep enum com.amazonaws.** {*; }
-keep enum com.fasterxml.jackson.databind.DeserializationFeature {*; }
-keep enum com.fasterxml.jackson.databind.MapperFeature {*; }
-keep enum com.fasterxml.jackson.databind.SerializationFeature {*; }
-keep enum org.apache.commons.text.translate.** {*; }
-keep enum org.apache.http.client.** {*; }
-keep enum org.apache.log4j.** {*; }

-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}
-keep class * {
    public enum **;
}

-dontwarn com.amazonaws.services.simpleemail.AWSJavaMailTransport
-dontwarn com.amazonaws.util.Base64
-dontwarn com.amazonaws.xray.javax.servlet.**
-dontwarn com.amazonaws.xray.strategy.**
-dontwarn com.fasterxml.jackson.databind.ext.Java7SupportImpl
-dontwarn com.fasterxml.jackson.dataformat.cbor.CBORGenerator
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**
-dontwarn lombok.**
-dontwarn org.apache.commons.lang3.concurrent.AbstractCircuitBreaker
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.commons.logging.impl.ServletContextCleaner
-dontwarn org.apache.commons.text.lookup.ScriptStringLookup
-dontwarn org.apache.http.**
-dontwarn org.apache.log4j.chainsaw.**
-dontwarn org.apache.log4j.lf5.**
-dontwarn org.apache.log4j.net.**
-dontwarn org.apache.log4j.or.jms.MessageRenderer
-dontwarn org.apache.log4j.rewrite.ReflectionRewritePolicy
#-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.logging.slf4j.EventDataConverter
-dontwarn org.joda.time.**
-dontwarn org.slf4j.MDC
-dontwarn org.slf4j.MarkerFactory
-dontwarn org.apache.logging.log4j.core.appender.mom.kafka.KafkaManager.**
-dontwarn org.apache.logging.log4j.core.async.**
-dontwarn org.apache.logging.log4j.core.jackson.**