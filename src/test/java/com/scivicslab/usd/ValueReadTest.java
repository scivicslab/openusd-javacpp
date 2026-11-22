package com.scivicslab.usd;

import static com.scivicslab.usd.global.usd.ReadAllChildrenNames;
import static com.scivicslab.usd.global.usd.ReadDouble;
import static com.scivicslab.usd.global.usd.ReadHasValue;
import static com.scivicslab.usd.global.usd.ReadString;
import static com.scivicslab.usd.global.usd.ReadStringArray;
import static com.scivicslab.usd.global.usd.ReadTargets;
import static com.scivicslab.usd.global.usd.ReadVec3d;
import static com.scivicslab.usd.global.usd.ReadWorldTranslation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading composed values back out of a stage: the strings, numbers, vectors, string arrays,
 * relationship targets and child names that a reader of a .usda needs, including what a stronger
 * layer's over changes and the prims that exist only as an over.
 */
class ValueReadTest {

    private static final String BASE = """
            #usda 1.0

            def Xform "diagram"
            {
                def Xform "nodes"
                {
                    def Xform "tab"
                    {
                        custom string label = "project1/chat-01"
                        custom token role = "title"
                        custom double width = 4.25
                        custom string[] bullets = ["first", "second"]
                        double3 xformOp:translate = (5, -3.2, 0)
                        uniform token[] xformOpOrder = ["xformOp:translate"]

                        def Xform "chat"
                        {
                            custom string label = ".chat"
                            double3 xformOp:translate = (2, 1, 0)
                            uniform token[] xformOpOrder = ["xformOp:translate"]
                        }
                    }
                }

                def Xform "edges"
                {
                    def Xform "e1"
                    {
                        rel from = </diagram/nodes/tab>
                        rel to = </diagram/nodes/tab/chat>
                    }
                }
            }
            """;

    private static final String HUMAN = """
            #usda 1.0
            (
                subLayers = [@./base.usda@]
            )

            over "diagram"
            {
                over "nodes"
                {
                    over "tab"
                    {
                        custom string label = "edited by a person"
                    }

                    over "gone"
                    {
                        custom string label = "points at nothing"
                    }
                }
            }
            """;

    private static List<String> list(StringVector v) {
        List<String> out = new ArrayList<>();
        for (long i = 0; i < v.size(); i++) {
            out.add(v.get(i).getString());
        }
        return out;
    }

    @Test
    void readsComposedValues(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("base.usda"), BASE);
        UsdStageRefPtr ref = UsdStage.Open(dir.resolve("base.usda").toString());
        UsdStage stage = ref.access();
        UsdPrim tab = stage.GetPrimAtPath(new SdfPath("/diagram/nodes/tab"));

        assertEquals("project1/chat-01", ReadString(tab.GetAttribute(new TfToken("label"))).getString());
        assertEquals("title", ReadString(tab.GetAttribute(new TfToken("role"))).getString());
        assertEquals(4.25, ReadDouble(tab.GetAttribute(new TfToken("width")), -1), 1e-12);
        assertEquals(-1, ReadDouble(tab.GetAttribute(new TfToken("height")), -1), 1e-12);
        assertEquals(List.of("first", "second"), list(ReadStringArray(tab.GetAttribute(new TfToken("bullets")))));

        GfVec3d t = ReadVec3d(tab.GetAttribute(new TfToken("xformOp:translate")));
        assertEquals(5, t.get(0).get(), 1e-12);
        assertEquals(-3.2, t.get(1).get(), 1e-12);

        UsdPrim chat = stage.GetPrimAtPath(new SdfPath("/diagram/nodes/tab/chat"));
        GfVec3d w = ReadWorldTranslation(chat);
        assertEquals(7, w.get(0).get(), 1e-12);
        assertEquals(-2.2, w.get(1).get(), 1e-12);

        UsdPrim edge = stage.GetPrimAtPath(new SdfPath("/diagram/edges/e1"));
        assertEquals(List.of("/diagram/nodes/tab"), list(ReadTargets(edge.GetRelationship(new TfToken("from")))));
        assertEquals(List.of("/diagram/nodes/tab/chat"), list(ReadTargets(edge.GetRelationship(new TfToken("to")))));

        assertTrue(ReadHasValue(tab.GetAttribute(new TfToken("label"))));
        assertFalse(ReadHasValue(tab.GetAttribute(new TfToken("height"))));
        ref.close();
    }

    @Test
    void readsOverridesAndOverOnlyPrims(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("base.usda"), BASE);
        Files.writeString(dir.resolve("human.usda"), HUMAN);
        UsdStageRefPtr ref = UsdStage.Open(dir.resolve("human.usda").toString());
        UsdStage stage = ref.access();

        UsdPrim tab = stage.GetPrimAtPath(new SdfPath("/diagram/nodes/tab"));
        assertEquals("edited by a person", ReadString(tab.GetAttribute(new TfToken("label"))).getString());

        UsdPrim nodes = stage.GetPrimAtPath(new SdfPath("/diagram/nodes"));
        assertEquals(List.of("tab", "gone"), list(ReadAllChildrenNames(nodes)));

        UsdPrim gone = stage.GetPrimAtPath(new SdfPath("/diagram/nodes/gone"));
        assertTrue(gone.IsValid());
        assertFalse(gone.IsDefined(), "a prim that exists only as an over is not defined");
        assertTrue(tab.IsDefined());
        ref.close();
    }
}
