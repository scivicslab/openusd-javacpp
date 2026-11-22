// Authoring values the bound Set<T> overloads do not cover.
//
// UsdAttribute::Set<T> is instantiated for scalars, strings, tokens and 3-vectors only; arrays
// (VtArray<T>) are not mapped. These inline functions write the array-valued attributes a slide
// editor changes, into the stage's current edit target.
#pragma once

#include "pxr/pxr.h"
#include "pxr/base/gf/vec3f.h"
#include "pxr/base/vt/array.h"
#include "pxr/usd/sdf/types.h"
#include "pxr/usd/usd/attribute.h"
#include "pxr/usd/usd/prim.h"

/** Sets primvars:displayColor to the single colour (r, g, b), creating the attribute when needed. */
inline bool WriteDisplayColor(const pxr::UsdPrim& prim, double r, double g, double b) {
    if (!prim) return false;
    pxr::UsdAttribute attr = prim.CreateAttribute(pxr::TfToken("primvars:displayColor"),
                                                  pxr::SdfValueTypeNames->Color3fArray, false);
    if (!attr) return false;
    pxr::VtArray<pxr::GfVec3f> colors(1, pxr::GfVec3f(static_cast<float>(r), static_cast<float>(g), static_cast<float>(b)));
    return attr.Set(colors);
}
