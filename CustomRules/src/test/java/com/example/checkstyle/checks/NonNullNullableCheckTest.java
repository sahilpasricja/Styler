package com.example.checkstyle.checks;

import com.example.customrules.NonNullNullableCheck;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean.OutputStreamOptions; // Keep this import
import com.puppycrawl.tools.checkstyle.api.AbstractCheck; // ADD THIS IMPORT

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NonNullNullableCheckTest {

    private final List<AuditEvent> auditEvents = Collections.synchronizedList(new ArrayList<>());
    private DefaultLogger logger;

    @BeforeEach
    void setUp() {
        auditEvents.clear();
        OutputStream dummyOutputStream = new ByteArrayOutputStream();

        logger = new DefaultLogger(dummyOutputStream, OutputStreamOptions.NONE) {
            @Override
            public void auditStarted(AuditEvent event) {
                // Do nothing
            }

            @Override
            public void auditFinished(AuditEvent event) {
                // Do nothing
            }

            @Override
            public void fileStarted(AuditEvent event) {
                // Do nothing
            }

            @Override
            public void fileFinished(AuditEvent event) {
                // Do nothing
            }

            @Override
            public void addError(AuditEvent event) {
                auditEvents.add(event);
            }

            @Override
            public void addException(AuditEvent event, Throwable throwable) {
                auditEvents.add(event);
                System.err.println("Checkstyle Exception during audit:");
                throwable.printStackTrace(System.err);
            }
        };
    }

    /**
     * Helper method to run a Checkstyle check on a given code snippet.
     * @param checkClass The custom Check class to run.
     * @param config The configuration for the check.
     * @param codeSnippet The Java code to check.
     * @return A list of reported violation messages (Strings).
     * @throws CheckstyleException If Checkstyle encounters an error.
     */
    // ****** THIS IS THE CORRECTED PART ******
    private List<String> runCheck(Class<? extends AbstractCheck> checkClass, // Changed Check to AbstractCheck
                                  Configuration config,
                                  String codeSnippet) throws CheckstyleException {
        // ****************************************

        auditEvents.clear();
        com.puppycrawl.tools.checkstyle.Checker checker = new com.puppycrawl.tools.checkstyle.Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());

        DefaultConfiguration checkerConfig = new DefaultConfiguration("Checker");
        DefaultConfiguration treeWalkerConfig = new DefaultConfiguration("TreeWalker");
        checkerConfig.addChild(treeWalkerConfig);

        DefaultConfiguration customCheckConfig = new DefaultConfiguration(checkClass.getName());
        treeWalkerConfig.addChild(customCheckConfig);

        if (config != null) {
            Properties props = new Properties();
            config.getProperties().forEach((k, v) -> props.setProperty(k.toString(), v.toString()));
            customCheckConfig.setProperties(props);
        }

        checker.configure(checkerConfig);
        checker.addListener(logger);

        FileText fileText = new FileText("Test.java", StandardCharsets.UTF_8.name(), List.of(codeSnippet.split("\\R")));
        List<FileText> fileTexts = Collections.singletonList(fileText);

        checker.process(fileTexts);
        checker.destroy();

        List<String> errors = new ArrayList<>();
        for (AuditEvent event : auditEvents) {
            errors.add(event.getMessage());
        }
        return errors;
    }

    // --- Test Cases (These remain the same) ---

    @Test
    void testFieldWithoutAnnotation() throws CheckstyleException {
        String code = """
            public class Test {
                private String myField;
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertThat(violations, hasSize(1));
        assertThat(violations.get(0), containsString("Variable 'myField' should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testFieldWithNonNullAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nonnull;
            public class Test {
                @Nonnull
                private String myField;
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testFieldWithNullableAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nullable;
            public class Test {
                @Nullable
                private String myField;
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testMethodParameterWithoutAnnotation() throws CheckstyleException {
        String code = """
            public class Test {
                public void doSomething(String param) {}
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertThat(violations, hasSize(1));
        assertThat(violations.get(0), containsString("Parameter 'param' should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testMethodParameterWithAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nonnull;
            public class Test {
                public void doSomething(@Nonnull String param) {}
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testMethodReturnWithoutAnnotation() throws CheckstyleException {
        String code = """
            public class Test {
                public String getValue() { return null; }
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertThat(violations, hasSize(1));
        assertThat(violations.get(0), containsString("Method 'getValue' return type should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testMethodReturnWithAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nonnull;
            public class Test {
                @Nonnull
                public String getValue() { return "hello"; }
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testVoidMethodNoReturnAnnotationNeeded() throws CheckstyleException {
        String code = """
            public class Test {
                public void doSomething() {}
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testLocalVariableWithoutAnnotation() throws CheckstyleException {
        String code = """
            public class Test {
                public void method() {
                    String local;
                }
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertThat(violations, hasSize(1));
        assertThat(violations.get(0), containsString("Variable 'local' should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testLocalVariableWithAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nonnull;
            public class Test {
                public void method() {
                    @Nonnull String local = "value";
                }
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testConstructorParameterWithoutAnnotation() throws CheckstyleException {
        String code = """
            public class Test {
                public Test(String param) {}
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertThat(violations, hasSize(1));
        assertThat(violations.get(0), containsString("Parameter 'param' should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testConstructorParameterWithAnnotation() throws CheckstyleException {
        String code = """
            import javax.annotation.Nonnull;
            public class Test {
                public Test(@Nonnull String param) {}
            }
            """;
        DefaultConfiguration config = new DefaultConfiguration(NonNullNullableCheck.class.getName());
        List<String> violations = runCheck(NonNullNullableCheck.class, config, code);

        assertTrue(violations.isEmpty());
    }
}