use javy_plugin_api::{
    Config,
    javy::{Runtime, quickjs::prelude::Func},
    javy_plugin,
};

wit_bindgen::generate!({ world: "javy-plugin", generate_all });

fn config() -> Config {
    Config::default()
}

fn modify_runtime(runtime: Runtime) -> Runtime {
    // You can modify the runtime here if needed.
    runtime.context().with(|ctx| {
        ctx.globals().set("plugin", true).unwrap();
        ctx.globals()
            .set(
                "func",
                Func::from(|| {
                    crate::imported_function();
                }),
            )
            .unwrap();
    });

    runtime
}

struct Component;

javy_plugin!("javy-plugin", Component, config, modify_runtime);

export!(Component);
