// Reading composed values back out of a stage.
//
// The bound authoring surface writes values (UsdAttribute::Set<T>) but cannot read them: Get<T> and
// VtValue::Get<T> are templates the parser does not instantiate. A reader of a .usda -- a renderer,
// an editor -- needs the composed string, number, vector, string array, relationship targets and
// child names of a prim. These inline functions return them in types the binding already maps
// (std::string, double, GfVec3d, std::vector<std::string>), so Java sees them as static methods of
// com.scivicslab.usd.global.usd.
#pragma once

#include <string>
#include <vector>

#include "pxr/pxr.h"
#include "pxr/base/gf/matrix4d.h"
#include "pxr/base/gf/vec3d.h"
#include "pxr/base/gf/vec3f.h"
#include "pxr/base/vt/array.h"
#include "pxr/usd/sdf/layer.h"
#include "pxr/usd/sdf/path.h"
#include "pxr/usd/sdf/primSpec.h"
#include "pxr/usd/usd/attribute.h"
#include "pxr/usd/usd/prim.h"
#include "pxr/usd/usd/relationship.h"
#include "pxr/usd/usd/timeCode.h"
#include "pxr/usd/usdGeom/xformable.h"

/** True when the attribute has a composed value (authored or fallback). */
inline bool ReadHasValue(const pxr::UsdAttribute& attr) {
    return attr && attr.HasValue();
}

/** The composed value of a string or token attribute; empty when there is none or the type differs. */
inline std::string ReadString(const pxr::UsdAttribute& attr) {
    if (!attr) return std::string();
    pxr::VtValue v;
    if (!attr.Get(&v)) return std::string();
    if (v.IsHolding<std::string>()) return v.UncheckedGet<std::string>();
    if (v.IsHolding<pxr::TfToken>()) return v.UncheckedGet<pxr::TfToken>().GetString();
    return std::string();
}

/** The composed value of a double, float or int attribute; fallback when there is none. */
inline double ReadDouble(const pxr::UsdAttribute& attr, double fallback) {
    if (!attr) return fallback;
    pxr::VtValue v;
    if (!attr.Get(&v)) return fallback;
    if (v.IsHolding<double>()) return v.UncheckedGet<double>();
    if (v.IsHolding<float>()) return v.UncheckedGet<float>();
    if (v.IsHolding<int>()) return v.UncheckedGet<int>();
    return fallback;
}

/** The composed value of a double3 or float3 attribute; (0, 0, 0) when there is none. */
inline pxr::GfVec3d ReadVec3d(const pxr::UsdAttribute& attr) {
    if (!attr) return pxr::GfVec3d(0, 0, 0);
    pxr::VtValue v;
    if (!attr.Get(&v)) return pxr::GfVec3d(0, 0, 0);
    if (v.IsHolding<pxr::GfVec3d>()) return v.UncheckedGet<pxr::GfVec3d>();
    if (v.IsHolding<pxr::GfVec3f>()) return pxr::GfVec3d(v.UncheckedGet<pxr::GfVec3f>());
    return pxr::GfVec3d(0, 0, 0);
}

/** The composed value of a string[] or token[] attribute; empty when there is none. */
inline std::vector<std::string> ReadStringArray(const pxr::UsdAttribute& attr) {
    std::vector<std::string> out;
    if (!attr) return out;
    pxr::VtValue v;
    if (!attr.Get(&v)) return out;
    if (v.IsHolding<pxr::VtArray<std::string>>()) {
        for (const auto& s : v.UncheckedGet<pxr::VtArray<std::string>>()) out.push_back(s);
    } else if (v.IsHolding<pxr::VtArray<pxr::TfToken>>()) {
        for (const auto& t : v.UncheckedGet<pxr::VtArray<pxr::TfToken>>()) out.push_back(t.GetString());
    }
    return out;
}

/** The composed targets of a relationship, as path strings in order. */
inline std::vector<std::string> ReadTargets(const pxr::UsdRelationship& rel) {
    std::vector<std::string> out;
    if (!rel) return out;
    pxr::SdfPathVector targets;
    rel.GetTargets(&targets);
    for (const auto& p : targets) out.push_back(p.GetString());
    return out;
}

/** The names of every child of the prim in order, including prims that exist only as an over. */
inline std::vector<std::string> ReadAllChildrenNames(const pxr::UsdPrim& prim) {
    std::vector<std::string> out;
    if (!prim) return out;
    for (const auto& t : prim.GetAllChildrenNames()) out.push_back(t.GetString());
    return out;
}

/** The translation part of the prim's local-to-world transform at the default time. */
inline pxr::GfVec3d ReadWorldTranslation(const pxr::UsdPrim& prim) {
    if (!prim) return pxr::GfVec3d(0, 0, 0);
    pxr::UsdGeomXformable xf(prim);
    if (!xf) return pxr::GfVec3d(0, 0, 0);
    return xf.ComputeLocalToWorldTransform(pxr::UsdTimeCode::Default()).ExtractTranslation();
}

/** True when the prim has a composed primvars:displayColor with at least one entry. */
inline bool ReadHasDisplayColor(const pxr::UsdPrim& prim) {
    if (!prim) return false;
    pxr::UsdAttribute attr = prim.GetAttribute(pxr::TfToken("primvars:displayColor"));
    pxr::VtValue v;
    if (!attr || !attr.Get(&v) || !v.IsHolding<pxr::VtArray<pxr::GfVec3f>>()) return false;
    return !v.UncheckedGet<pxr::VtArray<pxr::GfVec3f>>().empty();
}

/** The first entry of the prim's composed primvars:displayColor; (-1, -1, -1) when there is none. */
inline pxr::GfVec3d ReadDisplayColor(const pxr::UsdPrim& prim) {
    if (!ReadHasDisplayColor(prim)) return pxr::GfVec3d(-1, -1, -1);
    pxr::VtValue v;
    prim.GetAttribute(pxr::TfToken("primvars:displayColor")).Get(&v);
    return pxr::GfVec3d(v.UncheckedGet<pxr::VtArray<pxr::GfVec3f>>()[0]);
}

/** The names of the properties that have an opinion in some layer, in USD's order. */
inline std::vector<std::string> ReadAuthoredPropertyNames(const pxr::UsdPrim& prim) {
    std::vector<std::string> out;
    if (!prim) return out;
    for (const auto& t : prim.GetAuthoredPropertyNames()) out.push_back(t.GetString());
    return out;
}

/**
 * Where the opinions on a prim come from, strongest first: one "realPath\tpathInThatLayer" per
 * prim spec. A prim brought in by a reference names the referenced layer and its own path there.
 */
inline std::vector<std::string> ReadPrimStack(const pxr::UsdPrim& prim) {
    std::vector<std::string> out;
    if (!prim) return out;
    for (const auto& spec : prim.GetPrimStack()) {
        out.push_back(spec->GetLayer()->GetRealPath() + "\t" + spec->GetPath().GetString());
    }
    return out;
}
