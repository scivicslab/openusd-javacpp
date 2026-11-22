package com.scivicslab.usd;

import static com.scivicslab.usd.global.usd.UsdRegisterPluginPath;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs in its own JVM with PXR_PLUGINPATH_NAME set to empty (the "plugin-path" surefire execution):
 * a program launched without that variable tells OpenUSD where plugInfo.json is before it opens a
 * stage, and can then write and read a .usda.
 */
class PluginPathTest {

    @Test
    void aProgramWithoutTheEnvironmentVariableRegistersThePluginsItself(@TempDir Path dir) throws Exception {
        String env = System.getenv("PXR_PLUGINPATH_NAME");
        assertTrue(env == null || env.isBlank(), "this test runs without PXR_PLUGINPATH_NAME: " + env);
        String plugins = System.getProperty("usd.plugins");
        assertTrue(UsdRegisterPluginPath(plugins), "plugins registered from " + plugins);

        Path file = dir.resolve("a.usda");
        UsdStageRefPtr ref = UsdStage.CreateNew(file.toString());
        ref.access().DefinePrim(new SdfPath("/a"), new TfToken("Xform"));
        ref.access().Save();
        ref.close();
        assertTrue(Files.readString(file).contains("def Xform \"a\""));
        UsdStageRefPtr again = UsdStage.Open(file.toString());
        assertTrue(again.access().GetPrimAtPath(new SdfPath("/a")).IsValid());
        again.close();
    }
}
