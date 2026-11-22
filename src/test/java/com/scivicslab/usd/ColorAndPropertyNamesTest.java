package com.scivicslab.usd;

import static com.scivicslab.usd.global.usd.ReadAuthoredPropertyNames;
import static com.scivicslab.usd.global.usd.ReadDisplayColor;
import static com.scivicslab.usd.global.usd.ReadHasDisplayColor;
import static com.scivicslab.usd.global.usd.WriteDisplayColor;
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

/** A colour written as one primvars:displayColor entry into the edit target, read back composed. */
class ColorAndPropertyNamesTest {

    @Test
    void writesAColourIntoTheRootLayerAndReadsItBack(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("base.usda"), """
                #usda 1.0
                def Xform "bar"
                {
                    custom string label = "b"
                }
                """);
        Files.writeString(dir.resolve("human.usda"), """
                #usda 1.0
                (
                    subLayers = [@./base.usda@]
                )
                """);
        SdfLayerRefPtr human = SdfLayer.FindOrOpen(dir.resolve("human.usda").toString());
        UsdStageRefPtr ref = UsdStage.Open(dir.resolve("human.usda").toString());
        UsdStage stage = ref.access();
        stage.SetEditTarget(stage.GetEditTargetForLocalLayer(stage.GetRootLayer()));

        assertTrue(stage.GetPrimAtPath(new SdfPath("/bar")).IsDefined(), "the base layer defines /bar");
        UsdPrim bar = stage.OverridePrim(new SdfPath("/bar"));
        assertFalse(ReadHasDisplayColor(bar));
        assertTrue(WriteDisplayColor(bar, 0.8, 0.2, 0.2));
        assertTrue(ReadHasDisplayColor(bar));
        GfVec3d c = ReadDisplayColor(bar);
        assertEquals(0.8, c.get(0).get(), 1e-6);
        assertEquals(0.2, c.get(1).get(), 1e-6);

        List<String> names = new ArrayList<>();
        StringVector v = ReadAuthoredPropertyNames(bar);
        for (long i = 0; i < v.size(); i++) names.add(v.get(i).getString());
        assertEquals(List.of("label", "primvars:displayColor"), names);

        String base = Files.readString(dir.resolve("base.usda"));
        assertTrue(human.access().Save());
        assertEquals(base, Files.readString(dir.resolve("base.usda")), "the weaker layer is not touched");
        String text = Files.readString(dir.resolve("human.usda"));
        assertTrue(text.contains("over \"bar\""), text);
        assertTrue(text.contains("color3f[] primvars:displayColor = [(0.8, 0.2, 0.2)]"), text);
        ref.close();
    }
}
