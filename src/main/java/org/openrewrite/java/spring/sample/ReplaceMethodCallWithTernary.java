package org.openrewrite.java.spring.sample;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMethodCallWithTernary extends Recipe {

    @Option(displayName = "Method pattern",
            description = "Method pattern that needs to be convert to ternary with casting.",
            example = "*..* getReasonPhrase()")
    String methodPattern;

    @Option(displayName = "Fully-qualified type name",
            description = "The fully qualified class name.",
            example = "org.springframework.http.HttpStatus")
    String fullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Replace method call with ternary";
    }

    @Override
    public String getDescription() {
        return "Replace method call with ternary.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new ReplaceMethodCallWithTernaryVisitor(new MethodMatcher(methodPattern)));
    }

    private class ReplaceMethodCallWithTernaryVisitor extends JavaVisitor<ExecutionContext> {

        private final MethodMatcher methodMatcher;

        public ReplaceMethodCallWithTernaryVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J visitTernary(J.Ternary ternary, ExecutionContext executionContext) {
            return super.visitTernary(ternary, executionContext);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            String shortQualifiedTypeName = fullyQualifiedTypeName.substring(fullyQualifiedTypeName.lastIndexOf('.') + 1);
            StringBuilder wrappedShortQualifiedTypeName = getWrappedString(shortQualifiedTypeName);

            if (methodMatcher.matches(method)
                    && method.getMethodType().getDeclaringType().getFullyQualifiedName().equals(fullyQualifiedTypeName)
                    && !method.toString().contains(shortQualifiedTypeName)) {
                String template = "#{any(org.springframework.http.HttpStatus)} instanceof " + shortQualifiedTypeName + " ? " + "(" + wrappedShortQualifiedTypeName + " #{any(org.springframework.http.HttpStatus)})." + method.getSimpleName() + "()" + " : " + "\"not provided\"";
                maybeAddImport(fullyQualifiedTypeName);

                return JavaTemplate.builder(template)
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .imports(fullyQualifiedTypeName, fullyQualifiedTypeName)
                        .build()
                        .apply(
                                getCursor(),
                                method.getCoordinates().replace(),
                                ((J.MethodInvocation) method.getSelect()).getSelect(),
                                ((J.MethodInvocation) method.getSelect()).getSelect()
                        );

            }
            return method;
        }
    }

    private static StringBuilder getWrappedString(String str) {
        StringBuilder wrappedShortQualifiedTypeName = new StringBuilder();
        wrappedShortQualifiedTypeName.append("(").append(str).append(")");
        return wrappedShortQualifiedTypeName;
    }
}