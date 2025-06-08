package com.example.app;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.NonNull; // If using Lombok

public class MyApp {

    // VIOLATION: Field not annotated
    private String unannotatedField;

    // OK: Field annotated
    @Nonnull
    private String requiredField;

    // OK: Field annotated
    @Nullable
    private Integer optionalNumber;

    // OK: Field annotated with Lombok's @NonNull
    @NonNull
    private String lombokField;

    // VIOLATION: Constructor parameter not annotated
    public MyApp(String param1, @Nonnull String param2) {
        this.unannotatedField = param1;
        this.requiredField = param2;
        this.lombokField = "default";
    }

    // OK: All constructor parameters annotated
    public MyApp(@Nonnull String paramA, @Nullable String paramB, @NonNull String paramC) {
        this.unannotatedField = paramA;
        this.requiredField = paramB;
        this.lombokField = paramC;
    }

    // VIOLATION: Method return type not annotated, and local variable not annotated
    public String calculateSomething(@Nonnull String input) {
        // VIOLATION: Local variable not annotated
        String result = input + " processed";
        return result;
    }

    // OK: Method return type annotated
    @Nonnull
    public String getFormattedOutput(@Nullable String data) {
        // OK: Local variable annotated
        @Nonnull String output = data != null ? data : "N/A";
        return output;
    }

    // OK: Void method (no return type annotation required by the rule)
    public void doNothing(@NonNull String message) {
        System.out.println(message);
    }
}