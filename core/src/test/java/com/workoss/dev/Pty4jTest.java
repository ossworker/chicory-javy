package com.workoss.dev;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Pty4jTest {


    public static void main(String[] args) {
        new Pty4jTest().test01();
    }

    @Test
     void test01() {
        PtyProcess process = null;
        try {
            String[] cmd = {"/bin/sh", "-l"};
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");

            process = new PtyProcessBuilder().setCommand(cmd)
                    .setEnvironment(env)
                    .setConsole(false)
                    .start();

            OutputStream stdin = process.getOutputStream();

            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getInputStream();

            // ... work with the streams ...
            // 读取输出（示例）
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("" + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("" + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // 发送命令到终端
            stdin.write("ls -la\n".getBytes());
//            stdin.write("brew upgrade\n".getBytes());

//            stdin.write("exit\n".getBytes());
            stdin.flush();

            stdin.write("exit\n".getBytes());
            stdin.flush();

            // wait until the PTY child process is terminated
            boolean result = process.waitFor(10, TimeUnit.SECONDS);
            System.out.println(result);
            stdin.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            process.destroy();
        }
    }
}
