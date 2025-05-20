package com.workoss.dev;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChicoryJSTest {

    @Test
    public void basicUsage() {
        // Arrange
        var invoked = new AtomicBoolean(false);
        var chicoryJs =
                ChicoryJS.builder()
                        .withImportedFunction(
                                (str) -> {
                                    assertEquals("ciao", str);
                                    invoked.set(true);
                                    return "{ received: " + str + "}";
                                })
                        .build();

        // Act
        var codePtr =
                chicoryJs.compile(
                        "console.log(\"hello js world!!!\");"
                                + " console.error(java_imported_function(\"ciao\"));");
        chicoryJs.exec(codePtr);
        chicoryJs.free(codePtr);
        chicoryJs.close();

        // Assert
        assertTrue(invoked.get());
    }

    @Test
    public void javaFunctionInvocation() {
        // Arrange
        var chicoryJs =
                ChicoryJS.builder()
                        .withImportedFunction(
                                (str) -> {
                                    System.out.println("from here " + str);

                                    return "java_imported_function(\"from_java\");";
                                })
                        .build();

        // Act
        var codePtr = chicoryJs.compile("eval(java_imported_function(\"from_js\"));");
        //  var codePtr = chicoryJs.compile("console.error(java_imported_function(\"from_js\"));");
        chicoryJs.exec(codePtr);
        chicoryJs.free(codePtr);
        chicoryJs.close();

        // Assert
    }

    @Test
    public void javaFunctionInvocations() {

        ChicoryJS chicoryJs =
                ChicoryJS.builder()
                        .withImportedFunction(
                                (str) -> {
                                    System.out.println("from here " + str);
                                    return "java_imported_function(\"from_java\");";
                                })
                        .build();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            // Act
            var codePtr = chicoryJs.compile("eval(java_imported_function(\"from_js:\"+plugin+"+i+"));");
            chicoryJs.exec(codePtr);
            chicoryJs.free(codePtr);
        }
        chicoryJs.close();

        long end = System.currentTimeMillis();
        System.out.println("Java Function execution time: " + (end - start) + "ms");
    }
}
