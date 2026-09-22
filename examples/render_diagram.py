# Renders a diagram stage (see ActorTreeDiagram.java for the schema) to a PNG with Blender.
#
#   blender -b --python render_diagram.py -- diagram.usda diagram.png
#
# The stage is the source of truth: boxes come from prims under /diagram/nodes that carry a
# `label` attribute, arrows from prims under /diagram/edges with `from` / `to` relationships.
# World positions are taken from USD's own transform composition (UsdGeom.XformCache), so the
# file may nest prims freely.

import math
import sys
from pathlib import Path

import bpy
from pxr import Usd, UsdGeom

# Monospace: in a sans-serif proportional face the lowercase l, the capital I and the digit 1 are
# the same shape, and every label here is an identifier. Noto Sans Mono CJK JP carries CJK too.
FONT = str(Path.home() / ".local/share/fonts/NotoSansMonoCJKjp-Regular.otf")
COLORS = {
    "pane":     (0.97, 0.97, 0.98, 1),
    "control":  (0.91, 0.93, 0.96, 1),
    "content":  (1.00, 1.00, 1.00, 1),
    "actor":    (0.86, 0.91, 0.98, 1),
    "pojo":     (0.99, 0.90, 0.90, 1),
    "external": (0.90, 0.95, 0.88, 1),
    "file":     (0.94, 0.92, 0.98, 1),
}
BORDER = (0.35, 0.42, 0.52, 1)
INK = (0.12, 0.12, 0.14, 1)
LABEL_SIZE, POJO_SIZE, EDGE_LABEL_SIZE = 1.3, 0.95, 1.1
# Blender draws Noto Sans CJK about 0.6 x `size` tall, so line heights are set from the glyphs, not from `size`.
LABEL_LINE, POJO_LINE = 0.95, 0.72
PAD = 0.28                           # top / bottom padding inside a box
TREE = (0.62, 0.68, 0.76, 1)


def material(name, rgba):
    m = bpy.data.materials.get(name)
    if m is None:
        m = bpy.data.materials.new(name)
        m.diffuse_color = rgba
        m.use_nodes = False
    return m


def plane(name, x, y, w, h, z, mat):
    bpy.ops.mesh.primitive_plane_add(size=1, location=(x, y, z))
    o = bpy.context.object
    o.name = name
    o.scale = (w, h, 1)
    o.data.materials.append(mat)
    return o


def text(name, body, x, y, z, size, align="CENTER"):
    c = bpy.data.curves.new(name, type="FONT")
    c.body = body
    c.size = size
    c.align_x = align
    c.align_y = "CENTER"
    c.space_line = 0.74   # matches LABEL_LINE / LABEL_SIZE and POJO_LINE / POJO_SIZE
    c.font = bpy.data.fonts.load(FONT, check_existing=True)
    c.materials.append(material("ink", INK))
    o = bpy.data.objects.new(name, c)
    o.location = (x, y, z)
    bpy.context.collection.objects.link(o)
    return o


def clip_to_box(cx, cy, w, h, tx, ty):
    """Point on the border of the box centred at (cx, cy) in the direction of (tx, ty)."""
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    sx = (w / 2) / abs(dx) if dx else math.inf
    sy = (h / 2) / abs(dy) if dy else math.inf
    s = min(sx, sy)
    return cx + dx * s, cy + dy * s


def segment(name, x0, y0, x1, y1, z, thickness, mat):
    """A straight bar from (x0, y0) to (x1, y1)."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    if length < 1e-6:
        return None
    o = plane(name, (x0 + x1) / 2, (y0 + y1) / 2, length, thickness, z, mat)
    o.rotation_euler = (0, 0, math.atan2(dy, dx))
    return o


def head(name, x, y, ang, mat):
    """A triangle whose tip is at (x, y), pointing along ang."""
    h, half = 0.6, 0.26
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(
        [(x, y, 0.011),
         (x - h * math.cos(ang) - half * math.sin(ang), y - h * math.sin(ang) + half * math.cos(ang), 0.011),
         (x - h * math.cos(ang) + half * math.sin(ang), y - h * math.sin(ang) - half * math.cos(ang), 0.011)],
        [], [(0, 1, 2)])
    mesh.materials.append(mat)
    o = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(o)


def route(src, dst, offset=0.0):
    """Polyline from the border of src to the border of dst, using horizontal and vertical runs.

    Same row: one straight run. Target to the right: right, down/up, right. Target to the left:
    left along the source's row, then up/down into the target's bottom or top edge.
    """
    sx, sy, sw, sh = src["x"], src["y"], src["w"], src["h"]
    tx, ty, tw, th = dst["x"], dst["y"], dst["w"], dst["h"]
    if abs(tx - sx) < 0.8:
        # Same column: one straight vertical run between the facing edges.
        if ty < sy:
            return [(sx, sy - sh / 2), (tx, ty + th / 2)]
        return [(sx, sy + sh / 2), (tx, ty - th / 2)]
    if abs(ty - sy) < 0.8:
        if tx > sx:
            return [(sx + sw / 2, sy), (tx - tw / 2, ty)]
        return [(sx - sw / 2, sy), (tx + tw / 2, ty)]
    if tx > sx:
        x0, x1 = sx + sw / 2, tx - tw / 2
        # vertical run just outside the source box, so it never crosses a wide target box
        xm = min(x0 + 0.9 + offset, x1 - 0.3)
        return [(x0, sy), (xm, sy), (xm, ty), (x1, ty)]
    yend = ty - th / 2 if sy < ty else ty + th / 2
    return [(sx - sw / 2, sy), (tx, sy), (tx, yend)]


def arrow(name, points, mat):
    for i in range(len(points) - 1):
        (x0, y0), (x1, y1) = points[i], points[i + 1]
        # shorten the last run so the shaft ends under the head
        if i == len(points) - 2:
            dx, dy = x1 - x0, y1 - y0
            length = math.hypot(dx, dy)
            if length > 0.55:
                x1, y1 = x1 - dx / length * 0.55, y1 - dy / length * 0.55
        segment(f"{name}_s{i}", x0, y0, x1, y1, 0.01, 0.11, mat)
    (px, py), (qx, qy) = points[-2], points[-1]
    head(name + "_head", qx, qy, math.atan2(qy - py, qx - px), mat)


def main(usda, png):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    stage = Usd.Stage.Open(usda)
    xc = UsdGeom.XformCache()

    # Text size is a property of the figure, not of the renderer: a screen layout spans a wider
    # canvas than a structure diagram, so it needs bigger type to stay readable at the same
    # printed width. /diagram may carry labelSize / subSize / edgeLabelSize; these are the defaults.
    global LABEL_SIZE, POJO_SIZE, EDGE_LABEL_SIZE, LABEL_LINE, POJO_LINE
    LABEL_SIZE, POJO_SIZE, EDGE_LABEL_SIZE = 1.3, 0.95, 1.1
    root = stage.GetPrimAtPath("/diagram")
    for name, default in (("labelSize", LABEL_SIZE), ("subSize", POJO_SIZE), ("edgeLabelSize", EDGE_LABEL_SIZE)):
        if root and root.HasAttribute(name) and root.GetAttribute(name).Get() is not None:
            value = root.GetAttribute(name).Get()
            if name == "labelSize":
                LABEL_SIZE = value
            elif name == "subSize":
                POJO_SIZE = value
            else:
                EDGE_LABEL_SIZE = value
    LABEL_LINE, POJO_LINE = LABEL_SIZE * 0.73, POJO_SIZE * 0.76

    nodes = {}
    for prim in stage.Traverse():
        path = prim.GetPath().pathString
        if not path.startswith("/diagram/nodes/") or not prim.HasAttribute("label"):
            continue
        t = xc.GetLocalToWorldTransform(prim).ExtractTranslation()
        pojo = prim.GetAttribute("pojo").Get() if prim.HasAttribute("pojo") else None
        label = prim.GetAttribute("label").Get()
        n1, n2 = label.count("\n") + 1, (pojo.count("\n") + 1) if pojo else 0
        # A node with an authored `height` is a rectangle of a given size -- a pane in a screen
        # layout. Without one it is a box grown to fit its text -- a node in a structure diagram.
        height = prim.GetAttribute("height").Get() if prim.HasAttribute("height") else None
        nodes[path] = dict(
            x=t[0], y=t[1],
            label=label,
            pojo=pojo,
            kind=prim.GetAttribute("kind").Get() or "actor",
            w=prim.GetAttribute("width").Get() or 4.0,
            h=height if height else 2 * PAD + LABEL_LINE * n1 + POJO_LINE * n2,
            n1=n1, n2=n2,
            rect=height is not None,
        )

    border = material("border", BORDER)
    # Bigger first, so a nested pane is drawn on top of the one that contains it.
    for path, n in sorted(nodes.items(), key=lambda kv: -kv[1]["w"] * kv[1]["h"]):
        base = path.rsplit("/", 1)[1]
        z = 0.0 if not n["rect"] else min(0.3, 0.3 - n["w"] * n["h"] / 4000.0)
        plane(base + "_border", n["x"], n["y"], n["w"] + 0.12, n["h"] + 0.12, z - 0.01, border)
        plane(base + "_box", n["x"], n["y"], n["w"], n["h"], z, material("kind_" + n["kind"], COLORS[n["kind"]]))
        top = n["y"] + n["h"] / 2 - PAD
        if n["rect"]:
            # A pane is named in its top-left corner, the way a screen reads.
            left = n["x"] - n["w"] / 2 + PAD
            text(base + "_label", n["label"], left, top - LABEL_LINE * n["n1"] / 2, z + 0.02,
                 LABEL_SIZE, align="LEFT")
            if n["pojo"]:
                text(base + "_pojo", n["pojo"], left,
                     top - LABEL_LINE * n["n1"] - POJO_LINE * n["n2"] / 2, z + 0.02, POJO_SIZE, align="LEFT")
        else:
            text(base + "_label", n["label"], n["x"], top - LABEL_LINE * n["n1"] / 2, 0.02, LABEL_SIZE)
            if n["pojo"]:
                text(base + "_pojo", n["pojo"], n["x"],
                     top - LABEL_LINE * n["n1"] - POJO_LINE * n["n2"] / 2, 0.02, POJO_SIZE)

    # Containment: a thin grey run from a parent box's right edge to each child's left edge.
    tree = material("tree", TREE)
    for path, n in nodes.items():
        parent = path.rsplit("/", 1)[0]
        if parent in nodes:
            p = nodes[parent]
            # Nested rectangles already show containment; a line would only cross them.
            if n["rect"] and p["rect"]:
                continue
            x0, x1 = p["x"] + p["w"] / 2, n["x"] - n["w"] / 2
            xm = x0 + 0.35
            base = path.rsplit("/", 1)[1]
            segment(base + "_t0", x0, p["y"], xm, p["y"], 0.005, 0.05, tree)
            segment(base + "_t1", xm, p["y"], xm, n["y"], 0.005, 0.05, tree)
            segment(base + "_t2", xm, n["y"], x1, n["y"], 0.005, 0.05, tree)

    ink = material("ink", INK)
    seen = {}
    for prim in stage.Traverse():
        path = prim.GetPath().pathString
        if not path.startswith("/diagram/edges/") or path == "/diagram/edges":
            continue
        src = nodes[prim.GetRelationship("from").GetTargets()[0].pathString]
        dst = nodes[prim.GetRelationship("to").GetTargets()[0].pathString]
        # edges leaving the same box get their vertical runs spread 0.5 apart
        k = seen.get(id(src), 0)
        seen[id(src)] = k + 1
        pts = route(src, dst, offset=0.5 * k)
        name = path.rsplit("/", 1)[1]
        arrow(name, pts, ink)
        if prim.HasAttribute("label"):
            label = prim.GetAttribute("label").Get()
            if len(pts) == 2 and abs(pts[0][0] - pts[1][0]) < 1e-6:
                # vertical run: label beside it, left-aligned
                text(name + "_label", label, pts[0][0] + 0.3, (pts[0][1] + pts[1][1]) / 2, 0.02,
                     EDGE_LABEL_SIZE, align="LEFT")
            elif len(pts) == 4:
                # elbow: label beside the vertical run, left-aligned
                (xm, ya), (_, yb) = pts[1], pts[2]
                text(name + "_label", label, xm + 0.3, (ya + yb) / 2, 0.02, EDGE_LABEL_SIZE, align="LEFT")
            else:
                # straight or L-shaped: label above the first (horizontal) run
                (x0, y0), (x1, y1) = pts[0], pts[1]
                text(name + "_label", label, (x0 + x1) / 2, (y0 + y1) / 2 + 0.6, 0.02, EDGE_LABEL_SIZE)

    # Camera: orthographic, looking straight down, framing every box with a margin.
    xs = [n["x"] - n["w"] / 2 for n in nodes.values()] + [n["x"] + n["w"] / 2 for n in nodes.values()]
    ys = [n["y"] - n["h"] / 2 for n in nodes.values()] + [n["y"] + n["h"] / 2 for n in nodes.values()]
    margin = 1.0
    minx, maxx, miny, maxy = min(xs) - margin, max(xs) + margin, min(ys) - margin, max(ys) + margin
    width, height = maxx - minx, maxy - miny
    cam_data = bpy.data.cameras.new("cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = max(width, height)
    cam = bpy.data.objects.new("cam", cam_data)
    cam.location = ((minx + maxx) / 2, (miny + maxy) / 2, 20)
    bpy.context.collection.objects.link(cam)
    scene = bpy.context.scene
    scene.camera = cam

    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "FLAT"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_object_outline = False
    scene.display_settings.display_device = "sRGB"
    scene.view_settings.view_transform = "Standard"
    world = bpy.data.worlds.new("w")
    world.color = (1, 1, 1)
    scene.world = world
    px = 3000
    scene.render.resolution_x = px
    scene.render.resolution_y = int(px * height / width) if width >= height else px
    if width < height:
        scene.render.resolution_x = int(px * width / height)
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = False
    scene.render.image_settings.file_format = "PNG"
    scene.render.filepath = png
    bpy.ops.render.render(write_still=True)
    print("rendered", png, scene.render.resolution_x, "x", scene.render.resolution_y)


if __name__ == "__main__":
    # usda1 png1 usda2 png2 ... : one Blender start-up draws every figure that needs it.
    args = sys.argv[sys.argv.index("--") + 1:]
    for i in range(0, len(args), 2):
        main(args[i], args[i + 1])
