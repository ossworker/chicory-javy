package com.workoss.dev;


import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.types.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;


//@WasmModuleInterface("file:D:/IDE/ideaProjects/chicory-javy/javy-plugin/target/wasm32-wasip2/release/javy_plugin.min.wasm")
@WasmModuleInterface("file:/D:/IDE/ideaProjects/chicory-javy/javy-plugin/target/wasm32-wasip2/release/javy_plugin.min.wasm")
public class ChicoryJS implements AutoCloseable {
    private static final int ALIGNMENT = 1;
    private final WasiOptions wasiOptions = WasiOptions.builder().inheritSystem().build();
    private final WasiPreview1 wasi = WasiPreview1.builder().withOptions(wasiOptions).build();
    private final Instance instance;
    private final ChicoryJS_ModuleExports exports;

    private final Function<String, String> importFun;


    public static Builder builder() {
        return new Builder();
    }


    private long[] importedFunction(Instance instance, long[] args) {
        int ptr = (int) args[0];
        int len = (int) args[1];

        byte[] bytes = instance.memory().readBytes(ptr, len);
        String str = new String(bytes, StandardCharsets.UTF_8);

        String returnStr = this.importFun.apply(str);
        byte[] returnBytes = returnStr.getBytes(StandardCharsets.UTF_8);
        int returnPtr = exports.cabiRealloc(0, 0, ALIGNMENT, returnBytes.length);
        exports.memory().write(returnPtr, returnBytes);

        int LEN = 8;
        int widePtr =
                exports.cabiRealloc(
                        0, // original_ptr
                        0, // original_size
                        ALIGNMENT, // alignment
                        LEN // new size
                );

        instance.memory().writeI32(widePtr, returnPtr);
        instance.memory().writeI32(widePtr + 4, returnBytes.length);

        return new long[]{widePtr};
    }

    private ChicoryJS(Function<String, String> importFun) {
        this.importFun = importFun;
        instance = Instance.builder(JavyPlugin.load())
                .withMemoryFactory(ByteArrayMemory::new)
                .withMachineFactory(JavyPlugin::create)
                .withImportValues(

                        ImportValues.builder()
                                .addFunction(wasi.toHostFunctions())
//                                .addFunction(new HostFunction(
//                                        "chicory",
//                                        "imported_function",
//                                        List.of(ValueType.I32, ValueType.I32),
//                                        List.of(ValueType.I32),
//                                        this::importedFunction
//                                ))
                                .build()
                )
                .withUnsafeExecutionListener((machine, event) -> System.out.println("current instruction: " + machine + ", stack size: " + event.size()))
                .build();
        exports = new ChicoryJS_ModuleExports(instance);
        exports.initializeRuntime();
    }

    public int compile(String js) {
        byte[] jsBytes = js.getBytes(StandardCharsets.UTF_8);
        int ptr = exports.cabiRealloc(0, 0, ALIGNMENT, jsBytes.length);

        exports.memory().write(ptr, jsBytes);
        int compileSrc = exports.compileSrc(ptr, jsBytes.length);
        //32
        return exports.memory().readInt(compileSrc);
    }

    public void exec(int codePtr) {
        int codeLength = exports.memory().readInt(codePtr + 4);
        exports.invoke(codePtr, codeLength,0,0,0);
//        exports.invoke(
//                codePtr, //bytecode_ptr
//                codeLength, //bytecode_len
//                0, //fn_name_ptr
//                0 //fn_name_len
//        );

    }

    public void free(int codePtr) {
        int codeLength = exports.memory().readInt(codePtr + 4);
        exports.cabiRealloc(codePtr, codeLength, ALIGNMENT, 0);
    }


    @Override
    public void close() {
        wasi.close();
    }

    public static final class Builder {

        private Function<String, String> importedFunction;

        private Builder() {
        }

        public Builder withImportedFunction(Function<String, String> importedFunction) {
            this.importedFunction = importedFunction;
            return this;
        }

        public ChicoryJS build() {
            return new ChicoryJS(importedFunction);
        }

    }

}
