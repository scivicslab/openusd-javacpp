# openusd-javacpp

Java bindings for the authoring side of [OpenUSD](https://github.com/PixarAnimationStudios/OpenUSD),
generated with [JavaCPP](https://github.com/bytedeco/javacpp).

The OpenUSD API is the one industry-standard object model for describing a scene: prims in a tree,
typed attributes on them, relationships between them, and layers composed over one another. This
project makes that API callable from Java, so a program can author a `.usda` file directly instead
of printing one.

Scope is `Tf`, `Sdf`, `Usd` and `UsdGeom` — authoring and composition. Imaging (Hydra, `usdview`)
is not bound: a renderer is an external program that reads the file this writes.

## Requirements

| | |
|---|---|
| JDK | 21 or later (`maven.compiler.release` is 21) |
| Maven | 3.9 or later |
| OpenUSD | 26.08, built monolithic, installed under a prefix with `include/` and `lib/libusd_ms.so` |
| Platform | `linux-x86_64` |

Build OpenUSD with its own `build_usd.py`. The default prefix this project expects is
`$HOME/.local/openusd-26.08`; a different one is passed as `-Dusd.home=/your/prefix`.

## Build and install

```bash
git clone git@github.com:scivicslab/openusd-javacpp.git
cd openusd-javacpp
rm -rf target
mvn install
```

`mvn clean` is not used here: `rm -rf target` first, then `mvn install`. The build runs in four
stages — compile the preset, run the JavaCPP parser over the OpenUSD headers, add the generated
sources, then compile the JNI library.

Two artifacts are installed:

```
com.scivicslab:openusd-javacpp:0.1.0-SNAPSHOT                    the generated Java classes
com.scivicslab:openusd-javacpp:0.1.0-SNAPSHOT:linux-x86_64       the JNI library for this platform
```

## Use it from a Maven project

```xml
<dependency>
  <groupId>com.scivicslab</groupId>
  <artifactId>openusd-javacpp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.scivicslab</groupId>
  <artifactId>openusd-javacpp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <classifier>linux-x86_64</classifier>
</dependency>
```

At run time OpenUSD has to find its plugin registrations, which live in the prefix rather than in
the jar. Set `PXR_PLUGINPATH_NAME` to `<prefix>/lib/usd`.

## A first program

Save this as `hello-usd.jsh`. Each statement is on one line because jshell evaluates line by line.

```java
import com.scivicslab.usd.*;
import org.bytedeco.javacpp.BytePointer;

UsdStageRefPtr stageRef = UsdStage.CreateNew("/tmp/hello.usda");
UsdStage stage = stageRef.access();
SdfSchema schema = SdfSchema.GetInstance();
SdfValueTypeName stringType = schema.FindType(new TfToken("string"));

stage.DefinePrim(new SdfPath("/world"), new TfToken("Xform"));
UsdPrim box = stage.DefinePrim(new SdfPath("/world/box"), new TfToken("Xform"));
new UsdGeomXformable(box).AddTranslateOp().Set(new GfVec3d(1, 2, 3));
box.CreateAttribute(new TfToken("label"), stringType, true).Set(new BytePointer("hello"));

stage.Save();
stageRef.close();
System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of("/tmp/hello.usda")));
/exit
```

Run it:

```bash
M=$HOME/.m2/repository
V=0.1.0-SNAPSHOT
CP=$M/com/scivicslab/openusd-javacpp/$V/openusd-javacpp-$V.jar
CP=$CP:$M/com/scivicslab/openusd-javacpp/$V/openusd-javacpp-$V-linux-x86_64.jar
CP=$CP:$M/org/bytedeco/javacpp/1.5.11/javacpp-1.5.11.jar

PXR_PLUGINPATH_NAME=$HOME/.local/openusd-26.08/lib/usd \
  jshell -R--enable-native-access=ALL-UNNAMED --class-path "$CP" hello-usd.jsh
```

It prints what OpenUSD wrote:

```
#usda 1.0

def Xform "world"
{
    def Xform "box"
    {
        custom string label = "hello"
        double3 xformOp:translate = (1, 2, 3)
        uniform token[] xformOpOrder = ["xformOp:translate"]
    }
}
```

**Hold the `UsdStageRefPtr`.** `UsdStage.CreateNew(...).access()` alone lets the garbage collector
free the stage while prims obtained from it are still in use, and OpenUSD then reports
`Used expired 'Xform' prim`. Keep the ref in a variable for as long as the stage is used, and
`close()` it at the end.

## Figures from USD

`bin/make-diagrams` uses the binding to turn figure definitions into pictures. It walks
`~/works/doc_*` for `*.jsh`, runs the out-of-date ones through one jshell session to write `.usda`,
and renders those through `examples/render_diagram.py` in one Blender run.

```bash
bin/make-diagrams          # everything under ~/works/doc_*
bin/make-diagrams <dir>    # one directory
bin/make-diagrams -a       # rebuild regardless of timestamps
```

The `.usda` is the figure: boxes are `Xform` prims, containment is prim nesting, arrows are
relationships under `/diagram/edges`. What it looks like is decided by the renderer, so a figure
stays readable by a program — and by a language model — after it has been drawn.

## Known limits

- `VtArray<…>` is not bound: `vt/array.h` does not get through the JavaCPP parser, so array-valued
  attributes (`points`, `token[]`) cannot be written from Java yet.
- The plugin registrations are not bundled in the native jar, so `PXR_PLUGINPATH_NAME` has to be
  set by the caller.
- `linux-x86_64` only.

## License

Apache License 2.0. See [LICENSE](LICENSE).

OpenUSD itself is licensed separately by Pixar; this project links against it but does not
redistribute it.
