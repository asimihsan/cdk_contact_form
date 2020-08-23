package com.myorg;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class CdkContactFormApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkContactFormStack(app, "CdkContactFormStack");

        app.synth();
    }
}
