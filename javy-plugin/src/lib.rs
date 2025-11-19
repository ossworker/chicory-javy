//! Plugin used for testing. We need a plugin with slightly different behavior
//! to validate a plugin is actually used when it should be.

use std::{io::{self, Read}, process};

use javy_plugin_api::{
    import_namespace, javy::{quickjs::prelude::Func, Runtime}, Config
};

use crate::shared_config::SharedConfig;

mod shared_config;

wit_bindgen::generate!({ world: "javy-plugin", generate_all });

import_namespace!("javy-plugin-v1");

struct Component;

impl Guest for Component {

    
    fn config_schema() -> Vec<u8> {
        shared_config::config_schema()
    }

    #[allow(async_fn_in_trait)]
    fn compile_src(src:Vec<u8>,) -> Result<Vec<u8>,String> {
        javy_plugin_api::compile_src(&src).map_err(|e| e.to_string())
    }

    #[allow(async_fn_in_trait)]
    fn initialize_runtime() -> () {
        javy_plugin_api::initialize_runtime(config, modify_runtime).unwrap()
    }

    #[allow(async_fn_in_trait)]
    fn invoke(bytecode:Vec<u8>,function:Option<String>,) -> () {
        javy_plugin_api::invoke(&bytecode, function.as_deref()).unwrap_or_else(|e| {
            eprintln!("{e}");
            process::abort();
        })
    }
}


fn config() -> Config {
    let mut config = Config::default();
    config
        .text_encoding(true)
        .javy_stream_io(true)
        .simd_json_builtins(true);

    let mut config_bytes = vec![];
    let shared_config = match io::stdin().read_to_end(&mut config_bytes) {
        Ok(0) => None,
        Ok(_) => Some(SharedConfig::parse_from_json(&config_bytes).unwrap()),
        Err(e) => panic!("Error reading from stdin: {e}"),
    };
    if let Some(shared_config) = shared_config {
        shared_config.apply_to_config(&mut config);
    }
    config
}

fn modify_runtime(runtime: Runtime) -> Runtime {
    runtime.context().with(|ctx| {
        ctx.globals().set("plugin", true).unwrap();
        ctx.globals()
            .set(
                "func",
                Func::from(|| {
                    // crate::imported_function();
                    println!("Hello, world! func");
                }),
            )
            .unwrap();
    });
    runtime
}

// javy_plugin!("javy-plugin", Component, config, modify_runtime);

export!(Component);
