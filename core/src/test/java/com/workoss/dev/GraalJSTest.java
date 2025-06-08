package com.workoss.dev;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

public class GraalJSTest {

    @Test
    void test01(){
        String JS_CODE = "(function myFun(param){console.log('Hello ' + param + ' from JS'); return 'hello world'})";
        try (Context context = Context.newBuilder()
                .allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).build())
                .allowHostClassLookup(className -> true)
                .build()) {
            java.math.BigDecimal v = context.eval("js",
                                                  "var BigDecimal = Java.type('java.math.BigDecimal');" +
                                                          "BigDecimal.valueOf(10).pow(2)")
                    .asHostObject();
//            assert v.toString().equals("100000000000000000000");
            System.out.println(v);
        }


    }

    @Test
    void test02(){
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try (Context context = Context.create()) {
                String JS_CODE = "(function myFun(param){console.log('Hello ' + param + ' from JS'); return 'hello world'})";
                Value value = context.eval("js", JS_CODE);
                Value result = value.execute("world");
                System.out.println(result);
            }
        }
        System.out.println("cost "+(System.currentTimeMillis()-start)+"ms");

    }
}
