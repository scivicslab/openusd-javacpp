package com.scivicslab.usd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bytedeco.javacpp.BytePointer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * First end-to-end check of the binding: author a small prim tree through the
 * OpenUSD API, save it, and look at the .usda text OpenUSD wrote.
 */
class StageAuthoringTest {

    @Test
    void writesPrimTreeAsUsda(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("actors.base.usda");

        UsdStageRefPtr stageRef = UsdStage.CreateNew(file.toString());
        UsdStage stage = stageRef.access();
        assertTrue(stage != null && !stage.isNull(), "CreateNew returned a null stage");

        TfToken xform = new TfToken("Xform");
        stage.DefinePrim(new SdfPath("/chat_01"), xform);
        UsdPrim chat = stage.DefinePrim(new SdfPath("/chat_01/chat"), xform);
        UsdPrim log = stage.DefinePrim(new SdfPath("/chat_01/log"), xform);
        assertTrue(chat.IsValid());
        assertTrue(log.IsValid());

        SdfSchema schema = SdfSchema.GetInstance();
        SdfValueTypeName stringType = schema.FindType(new TfToken("string"));
        SdfValueTypeName double3Type = schema.FindType(new TfToken("double3"));

        assertTrue(chat.CreateAttribute(new TfToken("pojo"), stringType, true)
                .Set(new BytePointer("ChatSession")));
        assertTrue(log.CreateAttribute(new TfToken("pojo"), stringType, true)
                .Set(new BytePointer("MultiplexerAccumulator")));
        assertTrue(log.CreateAttribute(new TfToken("xformOp:translate"), double3Type, false)
                .Set(new GfVec3d(0, -3, 0)));

        stage.Save();

        String text = Files.readString(file);
        assertTrue(text.startsWith("#usda 1.0"), text);
        assertTrue(text.contains("def Xform \"chat_01\""), text);
        assertTrue(text.contains("def Xform \"chat\""), text);
        assertTrue(text.contains("custom string pojo = \"ChatSession\""), text);
        assertTrue(text.contains("double3 xformOp:translate = (0, -3, 0)"), text);
    }

    @Test
    void reopensWhatItWrote(@TempDir Path dir) {
        Path file = dir.resolve("roundtrip.usda");
        UsdStageRefPtr stageRef = UsdStage.CreateNew(file.toString());   // the ref owns the stage
        UsdStage stage = stageRef.access();
        stage.DefinePrim(new SdfPath("/root/child"), new TfToken("Xform"));
        stage.Save();

        UsdStageRefPtr reopenedRef = UsdStage.Open(file.toString());
        UsdStage reopened = reopenedRef.access();
        UsdPrim child = reopened.GetPrimAtPath(new SdfPath("/root/child"));
        assertTrue(child.IsValid());
        assertEquals("child", child.GetName().GetString().getString());
        assertEquals("/root/child", child.GetPath().GetString().getString());
        reopenedRef.close();
        stageRef.close();
    }
}
