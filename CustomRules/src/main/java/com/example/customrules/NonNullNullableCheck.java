package com.example.customrules;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom Checkstyle rule to enforce the presence of @NonNull or @Nullable
 * annotations on variable declarations, method parameters, and method return types.
 */
public class NonNullNullableCheck extends AbstractCheck {

    /**
     * Set of annotation names considered as NonNull or Nullable.
     * Extend this set with any other annotations you use for nullability (e.g., org.jetbrains.annotations.Nullable).
     */
    private static final Set<String> NULLABILITY_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "NonNull",
            "Nullable",
            "org.checkerframework.checker.nullness.qual.NonNull",
            "org.checkerframework.checker.nullness.qual.Nullable",
            "javax.annotation.Nonnull",
            "javax.annotation.Nullable",
            "lombok.NonNull" // Added lombok.NonNull as it's common
            // Add any other specific @NonNull or @Nullable annotations your project uses
    ));

    @Override
    public int[] getDefaultTokens() {
        // We are interested in inspecting variable definitions, parameters, and method definitions.
        // return get
        // append("AcceptableTokens` method, which is the full set of tokens this check can handle.
        return new int[] {
                TokenTypes.VARIABLE_DEF,      // For local variables and fields
                TokenTypes.PARAMETER_DEF,     // For method parameters
                TokenTypes.METHOD_DEF,        // For method return types
                TokenTypes.CTOR_DEF           // For constructor parameters
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.VARIABLE_DEF:
                checkVariableDef(ast);
                break;
            case TokenTypes.PARAMETER_DEF:
                checkParameterDef(ast);
                break;
            case TokenTypes.METHOD_DEF:
                checkMethodDef(ast);
                break;
            case TokenTypes.CTOR_DEF:
                checkCtorDef(ast);
                break;
            default:
                // Should not happen based on getDefaultTokens
                break;
        }
    }

    /**
     * Checks if a variable definition (field or local variable) has a nullability annotation.
     * @param ast The AST node for the variable definition.
     */
    private void checkVariableDef(DetailAST ast) {
        // Find the ANNOTATION_DEF child, if any
        DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers != null) {
            if (!hasNullabilityAnnotation(modifiers)) {
                log(ast.getLineNo(), ast.getColumnNo(),
                        "Variable ''{0}'' should be annotated with @NonNull or @Nullable.",
                        getVariableName(ast));
            }
        } else {
            // No modifiers, so definitely no annotation
            log(ast.getLineNo(), ast.getColumnNo(),
                    "Variable ''{0}'' should be annotated with @NonNull or @Nullable.",
                    getVariableName(ast));
        }
    }

    /**
     * Checks if a method parameter definition has a nullability annotation.
     * @param ast The AST node for the parameter definition.
     */
    private void checkParameterDef(DetailAST ast) {
        // Find the ANNOTATION_DEF child within MODIFIERS for parameters
        DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers != null) {
            if (!hasNullabilityAnnotation(modifiers)) {
                log(ast.getLineNo(), ast.getColumnNo(),
                        "Parameter ''{0}'' should be annotated with @NonNull or @Nullable.",
                        getParameterName(ast));
            }
        } else {
            // No modifiers, so definitely no annotation
            log(ast.getLineNo(), ast.getColumnNo(),
                    "Parameter ''{0}'' should be annotated with @NonNull or @Nullable.",
                    getParameterName(ast));
        }
    }

    /**
     * Checks if a method return type (or the method itself for void) has a nullability annotation.
     * @param ast The AST node for the method definition.
     */
    private void checkMethodDef(DetailAST ast) {
        // For method return types, the annotation is on the MODIFIERS of the METHOD_DEF
        DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers != null) {
            if (!hasNullabilityAnnotation(modifiers)) {
                // We also need to consider methods that return void.
                // If it's void and no annotation, it's fine.
                // If it's not void and no annotation, then report.
                DetailAST type = ast.findFirstToken(TokenTypes.TYPE);
                if (type != null) {
                    DetailAST ident = type.findFirstToken(TokenTypes.IDENT);
                    if (ident != null && !"void".equals(ident.getText())) {
                        log(ast.getLineNo(), ast.getColumnNo(),
                                "Method ''{0}'' return type should be annotated with @NonNull or @Nullable.",
                                getMethodName(ast));
                    }
                } else {
                    // This case is likely for void methods without a TYPE token,
                    // or other complex scenarios. For simplicity, we assume methods with a return type
                    // will have a TYPE token. If no TYPE token, and no annotation, it's a void method.
                    // If you want to enforce @NonNull for void methods, you would remove the 'if (type != null)' check.
                    // For now, only non-void methods need return type annotation.
                }
            }
        } else {
            // No modifiers, so definitely no annotation.
            // Check if it's a non-void method.
            DetailAST type = ast.findFirstToken(TokenTypes.TYPE);
            if (type != null) {
                DetailAST ident = type.findFirstToken(TokenTypes.IDENT);
                if (ident != null && !"void".equals(ident.getText())) {
                    log(ast.getLineNo(), ast.getColumnNo(),
                            "Method ''{0}'' return type should be annotated with @NonNull or @Nullable.",
                            getMethodName(ast));
                }
            }
        }
    }

    /**
     * Checks if a constructor parameter definition has a nullability annotation.
     * Constructor parameters are handled similarly to method parameters.
     * @param ast The AST node for the constructor definition.
     */
    private void checkCtorDef(DetailAST ast) {
        // Constructor parameters are children of PARAMETER_DEF under PARAMETERS.
        DetailAST parameters = ast.findFirstToken(TokenTypes.PARAMETERS);
        if (parameters != null) {
            DetailAST parameterDef = parameters.findFirstToken(TokenTypes.PARAMETER_DEF);
            while (parameterDef != null) {
                checkParameterDef(parameterDef); // Reuse the checkParameterDef logic
                parameterDef = parameterDef.getNextSibling();
            }
        }
    }


    /**
     * Helper method to check if the MODIFIERS AST node contains any of the nullability annotations.
     * @param modifiers The MODIFIERS AST node.
     * @return true if a nullability annotation is found, false otherwise.
     */
    private boolean hasNullabilityAnnotation(DetailAST modifiers) {
        // Iterate through all children of MODIFIERS, looking for ANNOTATION tokens
        DetailAST child = modifiers.getFirstChild();
        while (child != null) {
            if (child.getType() == TokenTypes.ANNOTATION) {
                DetailAST ident = child.findFirstToken(TokenTypes.IDENT);
                if (ident != null) {
                    String annotationName = ident.getText();
                    if (NULLABILITY_ANNOTATIONS.contains(annotationName) || isFullyQualifiedNullabilityAnnotation(annotationName)) {
                        return true;
                    }
                }
            }
            child = child.getNextSibling(); // Use getNextSibling() to move to the next token
        }
        return false;
    }


    /**
     * Checks if the given annotation name is a fully qualified nullability annotation.
     * This allows for direct comparison with the fully qualified names in NULLABILITY_ANNOTATIONS.
     * @param annotationName The name of the annotation.
     * @return true if it's a fully qualified nullability annotation, false otherwise.
     */
    private boolean isFullyQualifiedNullabilityAnnotation(String annotationName) {
        for (String fqcn : NULLABILITY_ANNOTATIONS) {
            if (fqcn.contains(".")) { // Only check FQCNs
                if (fqcn.equals(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Extracts the variable name from a VARIABLE_DEF AST node.
     */
    private String getVariableName(DetailAST ast) {
        DetailAST ident = ast.findFirstToken(TokenTypes.IDENT);
        return ident != null ? ident.getText() : "unknown";
    }

    /**
     * Extracts the parameter name from a PARAMETER_DEF AST node.
     */
    private String getParameterName(DetailAST ast) {
        DetailAST ident = ast.findFirstToken(TokenTypes.IDENT);
        return ident != null ? ident.getText() : "unknown";
    }

    /**
     * Extracts the method name from a METHOD_DEF AST node.
     */
    private String getMethodName(DetailAST ast) {
        DetailAST ident = ast.findFirstToken(TokenTypes.IDENT);
        return ident != null ? ident.getText() : "unknown";
    }
}