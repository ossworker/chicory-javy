use javy_plugin_api::{Config, import_namespace, javy::quickjs::prelude::Func};

import_namespace!("chicory_plugin");

#[link(wasm_import_module = "chicory")]
unsafe extern "C" {
    fn imported_function(ptr: *const u8, len: usize) -> *const u32;
}

fn call_the_import(text: String) -> String {
    let bytes: &[u8] = text.as_bytes();

    let return_str = unsafe {
        let wide_ptr = imported_function(bytes.as_ptr(), bytes.len());
        let [ptr, len] = std::slice::from_raw_parts(wide_ptr, 2) else {
            unreachable!()
        };
        let res = std::slice::from_raw_parts(*ptr as *const u8, *len as usize);
        std::str::from_utf8(res).unwrap().to_string()
    };

    return_str
}

#[unsafe(export_name = "initialize_runtime")]
pub extern "C" fn initialize_runtime() {
    let mut config = Config::default();
    config
        .event_loop(true)
        .text_encoding(true)
        .redirect_stdout_to_stderr(true)
        .javy_stream_io(true)
        .simd_json_builtins(true);

    javy_plugin_api::initialize_runtime(config, |runtime| {
        runtime.context().with(|ctx| {
            ctx.globals().set("plugin", true).unwrap();
            ctx.globals()
                .set(
                    "java_imported_function",
                    Func::from(|a: String| call_the_import(a)),
                )
                .unwrap();
        });
        runtime
    })
    .unwrap();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {}
}
