package org.openrewrite.java.spring.sample;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceMethodCallWithTernaryTest  implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
          .classpathFromResources(new InMemoryExecutionContext(), "spring-web-5.3.+"));
    }

    @Test
    @DocumentExample
    void shouldReplaceMethodInvocationByTernary1() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceMethodCallWithTernary("org.springframework..* getReasonPhrase()", "org.springframework.http.HttpStatus")),
          //language=java
          java(
            """
                    package com.example;

                    import org.springframework.http.HttpStatus;
                    import org.springframework.http.ResponseEntity;
                    import java.util.LinkedHashMap;
                    import java.util.Map;

                    public class TestSample {

                        private ResponseEntity<Object> buildErrorResponse(HttpStatus status) {
                             Map<String, Object> body = new LinkedHashMap<>();
                             body.put("status", status.value());
                             body.put("error", status.getReasonPhrase());
                             return new ResponseEntity<>(body, status);
                        }
                    }
                    """,
            """
                    package com.example;
                    
                    import org.springframework.http.HttpStatus;
                    import org.springframework.http.ResponseEntity;
                    import java.util.LinkedHashMap;
                    import java.util.List;
                    import java.util.Map;

                    public class TestSample {

                        private ResponseEntity<Object> buildErrorResponse(HttpStatus status) {
                             Map<String, Object> body = new LinkedHashMap<>();
                             body.put("status", status.value());
                             body.put("error", status instanceof HttpStatus ? ((HttpStatus) status).getReasonPhrase() : "not provided");
                             return null;
                        }
                    }
                    """)
        );
    }


    @Test
    void shouldReplaceMethodInvocationByTernary2() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceMethodCallWithTernary("org.springframework..* getReasonPhrase()", "org.springframework.http.HttpStatus")),
          //language=java
          java(
            """
                    package com.example;

                    import org.springframework.http.HttpStatus;
                    import org.springframework.http.ResponseEntity;
                    import org.springframework.web.client.HttpClientErrorException;

                    public class TestSample {

                        private void validate(ResponseEntity<Object> response) {
                             HttpStatus status = response.getStatusCode();
                             throw HttpClientErrorException.create(status, status.getReasonPhrase(), response.getHeaders(), null, null);
                        }
                    }
                    """,
            """
                    package com.example;
                    
                    import org.springframework.http.HttpStatus;
                    import org.springframework.http.ResponseEntity;
                    import org.springframework.web.client.HttpClientErrorException;

                    public class TestSample {

                        private void validate(ResponseEntity<Object> response) {
                             HttpStatus status = response.getStatusCode();
                            throw HttpClientErrorException.create(status, status instanceof HttpStatus ? ((HttpStatus) status).getReasonPhrase() : "not provided", response.getHeaders(), null, null);
                        }
                    }
                    """)
        );
    }

}