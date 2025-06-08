package com.example.app;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean.OutputStreamOptions;
import com.puppycrawl.tools.checkstyle.PropertyResolver; // NEW IMPORT

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration; // Ensure this is imported for ConfigurationLoader result

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CheckstyleIntegrationTest {

    private final List<AuditEvent> auditEvents = Collections.synchronizedList(new ArrayList<>());
    private DefaultLogger logger;

    @TempDir
    Path tempDir;

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

    private List<String> runCheckstyleOnFiles(File targetJavaFile) throws CheckstyleException, IOException {
        auditEvents.clear();

        Checker checker = new Checker();
        // This is crucial for your custom rule (NonNullNullableCheck) to be found
        // This is where the classloader for resolving custom modules happens now.
        checker.setModuleClassLoader(getClass().getClassLoader());

        // Get the path to your checkstyle.xml from the classpath.
        // It's in src/main/resources, so it will be on the classpath at runtime.
        String checkstyleConfigPath = getClass().getClassLoader().getResource("checkstyle.xml").getPath();

        // *** THE CRUCIAL CHANGE: Use the correct ConfigurationLoader method ***
        // We need a PropertyResolver. For simple cases, we can use a basic one.
        PropertyResolver propertyResolver = new PropertyResolver() {
            @Override
            public String resolve(String key)  {
                // No properties being overridden in this example, so just return null
                return null;
            }
        };

        // This overload only takes config path and a PropertyResolver
        Configuration config = ConfigurationLoader.loadConfiguration(
                checkstyleConfigPath,
                propertyResolver
        );
        // ******************************************************************

        // Set up the checker with the loaded config and our logger
        checker.configure(config);
        checker.addListener(logger);

        // Process the target Java file
        checker.process(Collections.singletonList(targetJavaFile));
        checker.destroy();

        List<String> errors = new ArrayList<>();
        for (AuditEvent event : auditEvents) {
            errors.add(event.getMessage());
        }
        return errors;
    }

    // --- Test Cases (remain the same) ---

    @Test
    void testInputJavaFileForViolations() throws CheckstyleException, IOException {
        File inputJavaFile = new File("src/test/java/input/Input.java");
        assertTrue(inputJavaFile.exists(), "Input.java file not found at expected path: " + inputJavaFile.getAbsolutePath());

        List<String> violations = runCheckstyleOnFiles(inputJavaFile);

        System.out.println("Violations found for Input.java:");
        violations.forEach(System.out::println);

        assertThat(violations, hasSize(5));
        assertThat(violations.get(0), containsString("Variable 'unannotatedField' should be annotated with @NonNull or @Nullable."));
        assertThat(violations.get(1), containsString("Parameter 'unannotatedParam' should be annotated with @NonNull or @Nullable."));
        assertThat(violations.get(2), containsString("Method 'getUnannotatedValue' return type should be annotated with @NonNull or @Nullable."));
        assertThat(violations.get(3), containsString("Variable 'unannotatedLocalVar' should be annotated with @NonNull or @Nullable."));
        assertThat(violations.get(4), containsString("Parameter 'data' should be annotated with @NonNull or @Nullable."));
    }

    @Test
    void testMyAppClassForViolations() throws CheckstyleException, IOException {
        File myAppJavaFile = new File("src/main/java/com/example/app/MyApp.java");
        assertTrue(myAppJavaFile.exists(), "MyApp.java file not found at expected path: " + myAppJavaFile.getAbsolutePath());

        List<String> violations = runCheckstyleOnFiles(myAppJavaFile);

        System.out.println("\nViolations found for MyApp.java:");
        violations.forEach(System.out::println);

        assertThat(violations, hasSize(5));
    }
}