package com.scivicslab.usd;

import static com.scivicslab.usd.global.usd.ReadPrimStack;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Where each opinion on a prim comes from: the layer's real path and the prim's path inside it. */
class PrimStackTest {

    @Test
    void aPrimBroughtInByAReferenceNamesTheLayersAndPathsItComesFrom(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("fig.usda"), """
                #usda 1.0

                def Xform "diagram"
                {
                    def Xform "nodes"
                    {
                        def Xform "a"
                        {
                            custom string label = "a"
                        }
                    }
                }
                """);
        Files.writeString(dir.resolve("fig.human.usda"), """
                #usda 1.0
                (
                    subLayers = [@./fig.usda@]
                )
                """);
        Files.writeString(dir.resolve("talk.usda"), """
                #usda 1.0

                def Xform "Deck"
                {
                    def Xform "s"
                    {
                        def Xform "Figure" (
                            prepend references = @./fig.human.usda@</diagram>
                        )
                        {
                        }
                    }
                }
                """);
        UsdStageRefPtr ref = UsdStage.Open(dir.resolve("talk.usda").toString());
        UsdPrim a = ref.access().GetPrimAtPath(new SdfPath("/Deck/s/Figure/nodes/a"));
        List<String> stack = new ArrayList<>();
        StringVector v = ReadPrimStack(a);
        for (long i = 0; i < v.size(); i++) stack.add(v.get(i).getString());

        String fig = dir.resolve("fig.usda").toRealPath().toString();
        assertEquals(List.of(fig + "\t/diagram/nodes/a"), stack);
        ref.close();
    }
}
