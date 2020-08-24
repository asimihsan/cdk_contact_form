## Initialize stacks

```
(cd cdk && cdk bootstrap)
```

## How to build

Without Proguard

```
(cd lambda && ./gradlew build) && \
(cd cdk && ./gradlew build) && \
(cd cdk && cdk deploy preprod-CdkContactFormStack)
```

With Proguard

```
(cd lambda && ./gradlew build) && \
(cd lambda && proguard @proguard.pro) && \
(cd cdk && ./gradlew build) && \
(cd cdk && cdk deploy preprod-CdkContactFormStack)
```

