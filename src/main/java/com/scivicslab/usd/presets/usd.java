package com.scivicslab.usd.presets;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

/**
 * JavaCPP preset for the OpenUSD core: Tf / Gf / Vt / Sdf / Usd / UsdGeom.
 *
 * <p>Only the authoring surface is mapped in this first version. Everything
 * else in the headers is skipped so the parser can get through them; the
 * skip list grows as we hit constructs JavaCPP cannot express.</p>
 *
 * <p>The native library is the monolithic {@code libusd_ms.so} produced by
 * {@code build_usd.py --build-monolithic --no-python --no-imaging}.</p>
 */
@Properties(
    value = @Platform(
        value = "linux-x86_64",
        compiler = "cpp17",
        include = {
            "pxr/pxr.h",
            "pxr/base/tf/refBase.h",
            "pxr/base/tf/weakBase.h",
            "pxr/base/tf/refPtr.h",
            "pxr/base/tf/weakPtr.h",
            "pxr/base/tf/declarePtrs.h",
            "pxr/base/tf/token.h",
            "pxr/base/gf/vec3f.h",
            "pxr/base/gf/vec3d.h",
            "pxr/base/vt/value.h",
            "pxr/usd/sdf/path.h",
            "pxr/usd/sdf/valueTypeName.h",
            "pxr/usd/sdf/schema.h",
            "pxr/usd/sdf/layer.h",
            "pxr/usd/usd/timeCode.h",
            "pxr/usd/usd/object.h",
            "pxr/usd/usd/property.h",
            "pxr/usd/usd/attribute.h",
            "pxr/usd/usd/relationship.h",
            "pxr/usd/usd/prim.h",
            "pxr/usd/usd/stage.h",
            "pxr/usd/usd/typed.h",
            "pxr/usd/usdGeom/tokens.h",
            "pxr/usd/usdGeom/imageable.h",
            "pxr/usd/usdGeom/xformOp.h",
            "pxr/usd/usdGeom/xformable.h",
            "pxr/usd/usdGeom/xform.h",
            // Must stay last: brings namespace pxr into scope for the generated JNI code.
            "openusd_javacpp.h",
        },
        link = "usd_ms",
        // libusd_ms.so needs the TBB built alongside it; preload so it is copied into the jar
        // and loaded before usd_ms.
        preload = "tbb@.2"
    ),
    target = "com.scivicslab.usd",
    global = "com.scivicslab.usd.global.usd"
)
public class usd implements InfoMapper {

    @Override
    public void map(InfoMap infoMap) {
        // --- Namespace and export macros --------------------------------------------
        // The real declarations live in the versioned namespace pxrInternal_v0_26__pxrReserved__;
        // pxr.h also declares `namespace pxr { using namespace <that>; }`. We tell the parser
        // the classes are in `pxr` so the generated JNI code says pxr::UsdStage, which resolves
        // in any OpenUSD version without baking the internal namespace name into this file.
        infoMap.put(new Info("PXR_NAMESPACE_OPEN_SCOPE").cppText("namespace pxr {"))
               .put(new Info("PXR_NAMESPACE_CLOSE_SCOPE").cppText("}"))
               .put(new Info("PXR_NAMESPACE_USING_DIRECTIVE").cppText(""))
               // Version / namespace macros from pxr.h would otherwise become Java constants
               // whose values are C++ identifiers.
               .put(new Info("PXR_NS", "PXR_INTERNAL_NS", "PXR_NS_GLOBAL", "PXR_USE_NAMESPACES",
                             "PXR_MAJOR_VERSION", "PXR_MINOR_VERSION", "PXR_PATCH_VERSION", "PXR_VERSION",
                             "PXR_PREFER_SAFETY_OVER_SPEED", "PXR_PYTHON_SUPPORT_ENABLED",
                             // Token-list macros consumed by TF_DECLARE_PUBLIC_TOKENS; not values.
                             "USD_TIME_CODE_TOKENS", "USDGEOM_TOKENS", "USDGEOM_XFORM_OP_TYPES", "SDF_VALUE_TYPE_NAME_TOKENS",
                             "SDF_FIELD_KEYS", "SDF_CHILDREN_KEYS", "SDF_VALUE_TYPES", "SDF_VALUE_ROLE_NAME_TOKENS",
                             "SDF_TOKENS", "SDF_PATH_TOKENS", "SDF_METADATA_DISPLAYGROUP_TOKENS",
                             "SDF_DECLARE_VALUE_TYPE_TRAITS", "SDF_VALUE_CPP_TYPE", "SDF_VALUE_CPP_ARRAY_TYPE",
                             "SDF_VALUE_CPP_ARRAY_EDIT_TYPE", "SDF_LOCAL",
                             "_SDF_ANGULAR_UNITS", "_SDF_DECLARE_UNIT_ENUM", "_SDF_DECLARE_UNIT_ENUMERANT",
                             "_SDF_DIMENSIONED_VALUE_TYPES", "_SDF_DIMENSIONLESS_UNITS", "_SDF_FOR_EACH_UNITS",
                             "_SDF_FOR_EACH_UNITS_IMPL", "_SDF_LENGTH_UNITS", "_SDF_SCALAR_VALUE_TYPES", "_SDF_UNITS",
                             "_SDF_UNITSLIST_CATEGORY", "_SDF_UNITSLIST_ENUM", "_SDF_UNITSLIST_TUPLES",
                             "_SDF_UNIT_NAME", "_SDF_UNIT_SCALE", "_SDF_UNIT_TAG").skip())
               // Compiler-pragma macros that sit between `template<...>` and `class`.
               .put(new Info("ARCH_PRAGMA_PUSH", "ARCH_PRAGMA_POP", "ARCH_PRAGMA_NON_EXPORTED_BASE_CLASS",
                             "ARCH_PRAGMA_DEPRECATED_POSIX_NAME", "ARCH_PRAGMA_UNUSED_FUNCTION",
                             "ARCH_PRAGMA_MAYBE_UNINITIALIZED", "ARCH_PRAGMA_SHIFT_TO_64_BITS",
                             "ARCH_PRAGMA_ZERO_SIZED_STRUCT", "ARCH_PRAGMA_NEEDS_EXPORT_INTERFACE",
                             "ARCH_PRAGMA_CONVERSION_FROM_SIZET", "ARCH_PRAGMA_MAY_NOT_BE_ALIGNED",
                             "ARCH_PRAGMA_UNSAFE_USE_OF_BOOL", "ARCH_PRAGMA_UNARY_MINUS_ON_UNSIGNED",
                             "ARCH_PRAGMA_FORCING_TO_BOOL", "ARCH_PRAGMA_MACRO_REDEFINITION",
                             "ARCH_PRAGMA_UNUSED_PRIVATE_FIELD", "ARCH_PRAGMA_INSTANTIATION_AFTER_SPECIALIZATION",
                             "ARCH_PRAGMA_STRINGOP_OVERFLOW", "ARCH_PRAGMA_PLACEMENT_NEW").cppTypes().annotations())
               .put(new Info("TF_API", "GF_API", "VT_API", "SDF_API", "USD_API", "USDGEOM_API",
                             "ARCH_API", "ARCH_EXPORT", "ARCH_IMPORT", "ARCH_HIDDEN",
                             "ARCH_CONSTRUCTOR", "ARCH_DESTRUCTOR").cppTypes().annotations());

        // --- Standard library --------------------------------------------------------
        infoMap.put(new Info("std::string").annotations("@StdString")
                        .valueTypes("BytePointer", "String")
                        .pointerTypes("@Cast({\"char*\", \"std::string*\"}) BytePointer"))
               .put(new Info("std::vector<std::string>").pointerTypes("StringVector").define())
               .put(new Info("std::map<std::string,std::string>", "SdfLayer::FileFormatArguments")
                        .pointerTypes("StringStringMap").define());

        // --- Smart pointers: TfRefPtr<UsdStage> is what CreateNew/Open return,
        //     TfWeakPtr<UsdStage> is what the schema Define() calls take. ------------
        infoMap.put(new Info("TfRefPtr<UsdStage>", "UsdStageRefPtr").pointerTypes("UsdStageRefPtr").define())
               .put(new Info("TfWeakPtr<UsdStage>", "UsdStagePtr").pointerTypes("UsdStagePtr").define())
               .put(new Info("TfRefPtr<SdfLayer>", "SdfLayerRefPtr").pointerTypes("SdfLayerRefPtr").define())
               .put(new Info("TfWeakPtr<SdfLayer>", "SdfLayerHandle").pointerTypes("SdfLayerHandle").define());

        // --- VtArray: vt/array.h does not go through the parser yet, so every method that
        //     takes or returns an array is left out for now. -----------------------------------
        infoMap.put(new Info("VtArray<TfToken>", "VtTokenArray", "VtArray<int>", "VtIntArray",
                             "VtArray<float>", "VtFloatArray", "VtArray<GfVec3f>", "VtVec3fArray",
                             "VtArray<GfVec3d>", "VtVec3dArray", "VtArray<GfVec4d>", "VtVec4dArray",
                             "VtArray<GfMatrix4d>", "VtMatrix4dArray", "VtArray<double>", "VtDoubleArray",
                             "VtArray<std::string>", "VtStringArray", "VtArray<bool>", "VtBoolArray").skip());

        // --- Templates we instantiate explicitly -------------------------------------
        infoMap.put(new Info("UsdGeomXformOp::Set<GfVec3d>").javaNames("Set"))
               .put(new Info("UsdGeomXformOp::Set<GfVec3f>").javaNames("Set"))
               .put(new Info("UsdGeomXformOp::Set<double>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<double>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<int>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<bool>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<std::string>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<TfToken>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<GfVec3d>").javaNames("Set"))
               .put(new Info("UsdAttribute::Set<GfVec3f>").javaNames("Set"));

        // --- Things the parser cannot or need not handle in this version -------------
        infoMap.put(new Info("TF_DECLARE_WEAK_AND_REF_PTRS", "TF_DECLARE_WEAK_PTRS", "TF_DECLARE_REF_PTRS",
                             "TF_DECLARE_PUBLIC_TOKENS", "TF_DEFINE_STACKED", "TF_MALLOC_TAG_NEW",
                             "SDF_DECLARE_HANDLES", "USD_API_TEMPLATE_CLASS", "USD_DECLARE_TYPE_TRAITS",
                             "TF_DECLARE_REF_PTRS_AND_WEAK_PTRS",
                             "TF_DECLARE_WEAK_POINTABLE_INTERFACE", "TF_IMPLEMENT_WEAK_POINTABLE_INTERFACE",
                             "TF_SUPPORTS_WEAKPTR", "TF_TRULY_SUPPORTS_WEAKPTR").skip())
               // Reference-count tracker hooks take TfRefBase*, which Java only sees as Pointer.
               .put(new Info("Tf_RefPtrTracker_FirstRef", "Tf_RefPtrTracker_LastRef", "Tf_RefPtrTracker_New",
                             "Tf_RefPtrTracker_Delete", "Tf_RefPtrTracker_Assign").skip())
               .put(new Info("TfHash", "TfHashAppend", "hash_value", "TfAnyWeakPtr", "TfNotice",
                             "boost::python", "pxr_boost::python", "std::type_info",
                             "UsdStage::_PendingChanges", "UsdStage::_GetPcpCache").skip())
               // Container / functor types used only by methods outside this version's scope.
               // Skipping the type makes the parser drop every method that mentions it.
               .put(new Info("std::set<SdfPath>", "SdfPathSet",
                             "std::set<SdfLayerHandle>", "SdfLayerHandleSet",
                             "std::pair<std::string,std::string>",
                             "std::pair<SdfPrimSpecHandle,SdfLayerOffset>",
                             "std::pair<SdfPropertySpecHandle,SdfLayerOffset>",
                             "std::function<bool(const TfToken&)>", "UsdPrim::PropertyPredicateFunc",
                             "TfSpan<SdfPath>").skip())
               // Types reachable from SdfLayer / Gf / UsdTimeCode that we do not expose yet.
               .put(new Info("SdfFileFormatConstPtr", "TfWeakPtr<const SdfFileFormat>", "SdfFileFormat",
                             "VtDictionary", "SdfPrimSpecHandle", "SdfPrimSpecHandleVector", "SdfPrimSpecView",
                             "SdfPropertySpecHandle", "SdfAssetPath", "SdfDataRefPtr", "SdfLayerHints",
                             "ArResolvedPath", "SdfSpecType", "SdfLayer::TraversalFunction", "TraversalFunction",
                             "SdfSpec", "SdfNameOrderProxy", "SdfLayerOffset", "SdfAllowed", "SdfRelocate",
                             "SdfSchemaBase::FieldDefinition", "SdfSchemaBase::SpecDefinition",
                             "SdfSchemaBase::_ValueTypeRegistrar", "SdfSchemaBase::_SpecDefiner",
                             "SdfSchemaBase::FieldDefinition::Validator", "Validator", "JsValue",
                             "GfVec3h", "GfVec3i", "GfTimeCode").skip())
               // Internal machinery of TfWeakBase (remnants, registration) is not part of the API.
               .put(new Info("Tf_Remnant", "Tf_RemnantPtr", "TfRefPtr<Tf_Remnant>", "TfWeakBase::_Register",
                             "TfWeakBase::_Remnant", "Tf_WeakBaseAccess", "Tf_ExpiryNotifier",
                             "TfWeakPtrFacade", "TfWeakPtrFacadeAccess", "Tf_SupportsWeakPtr",
                             "TfRefPtrTracker", "Tf_RefPtrTracker", "Tf_RefPtr_UniqueChangedCounter",
                             "Tf_RefPtr_Counter").skip());

        // --- Reference-counted classes: never constructed or deleted from Java ---------------
        // TfRefBase / TfWeakBase have protected destructors and exist only as bases; SdfLayer and
        // UsdStage are created through their static factories and owned by TfRefPtr.
        infoMap.put(new Info("TfRefBase", "TfWeakBase").pointerTypes("Pointer"))
               .put(new Info("SdfLayer", "UsdStage").purify())
               .put(new Info("SdfLayer::SdfLayer", "UsdStage::UsdStage").skip())
               // Forward-declared only in the headers we include.
               .put(new Info("UsdAttributeLimits", "UsdAttribute::GetLimits",
                             "UsdResolveTarget", "UsdVariantSets", "UsdVariantSet", "UsdPayloads",
                             "UsdReferences", "UsdInherits", "UsdSpecializes").skip())
               // GetPrefixes has an overload taking TfSpan that the parser turns into a field;
               // SdfTupleDimensions is built from a size_t[2] the generator cannot marshal.
               .put(new Info("SdfPath::GetPrefixes", "SdfTupleDimensions", "SdfValueTypeName::GetDimensions").skip());

        // --- Enums and integer typedefs: pass as int rather than generating enum classes ---
        infoMap.put(new Info("SdfVariability", "SdfSpecifier", "UsdListPosition", "UsdLoadPolicy",
                             "UsdInterpolationType", "UsdSchemaVersion", "UsdPrim::VersionPolicy",
                             "UsdStage::InitialLoadSet", "UsdSchemaKind", "UsdGeomXformOp::Type",
                             "UsdGeomXformOp::Precision").cast().valueTypes("int"));
        // UsdGeom: matrices, purpose/visibility queries and the xformOp vector are outside this version.
        infoMap.put(new Info("GfMatrix4d", "GfBBox3d", "TfStaticData", "UsdAttributeQuery",
                             "UsdGeomXformable::_ValidAttributeTagType", "UsdGeomXformOp::_ValidAttributeTagType", "_ValidAttributeTagType",
                             "UsdGeomXformOp::UsdGeomXformOp",
                             "UsdGeomXformable::XformQuery", "std::vector<UsdGeomXformOp>",
                             "UsdGeomImageable::PurposeInfo", "UsdGeomXformCache",
                             // token structs: const vector fields cannot get a setter; use new TfToken("…") instead
                             "UsdGeomTokensType", "UsdGeomTokens", "UsdGeomTokensType::allTokens",
                             // returns const TfTokenVector&; the generated adapter drops the const
                             "UsdSchemaBase::GetSchemaAttributeNames", "UsdTyped::GetSchemaAttributeNames",
                             "UsdGeomImageable::GetSchemaAttributeNames", "UsdGeomXformable::GetSchemaAttributeNames",
                             "UsdGeomXform::GetSchemaAttributeNames", "UsdAPISchemaBase::GetSchemaAttributeNames").skip());

        // --- Smart-pointer template instantiations: keep the Java class, drop its C++ constructors
        //     (copy and move erase to the same Java signature). Instances only ever come back
        //     from CreateNew/Open/GetRootLayer, so Java never needs to construct one. -----------
        infoMap.put(new Info("TfRefPtr<UsdStage>::TfRefPtr", "TfRefPtr<SdfLayer>::TfRefPtr",
                             "TfWeakPtr<UsdStage>::TfWeakPtr", "TfWeakPtr<SdfLayer>::TfWeakPtr",
                             "TfNullPtrType", "TfCallContext",
                             // Safe-bool idiom conversion operators; Java has not()/isNull() instead.
                             "UnspecifiedBoolType", "TfRefPtr<UsdStage>::UnspecifiedBoolType",
                             "TfRefPtr<SdfLayer>::UnspecifiedBoolType", "TfWeakPtr<UsdStage>::UnspecifiedBoolType",
                             "TfWeakPtr<SdfLayer>::UnspecifiedBoolType", "TfWeakPtrFacade::UnspecifiedBoolType").skip());

        // --- UsdPrim: applied-schema family and prim iteration are out of scope for now ----------
        infoMap.put(new Info("UsdPrim::CanApplyAPI", "UsdPrim::ApplyAPI", "UsdPrim::RemoveAPI", "UsdPrim::HasAPI",
                             "UsdPrim::AddAppliedSchema", "UsdPrim::RemoveAppliedSchema", "UsdPrim::GetAppliedSchemas",
                             "UsdPrim::IsA", "UsdPrim::IsInFamily", "UsdPrim::GetPrimTypeInfo",
                             "UsdPrim::HasAPIInFamily", "UsdPrim::CanApplyAPIInFamily", "UsdPrim::ApplyAPIInFamily",
                             "UsdPrim::RemoveAPIInFamily", "UsdPrim::GetVersionIfHasAPIInFamily",
                             "UsdPrim::GetVersionIfIsInFamily", "UsdSchemaRegistry::VersionPolicy", "VersionPolicy",
                             "UsdPrim::GetChildren", "UsdPrim::GetAllChildren", "UsdPrim::GetFilteredChildren",
                             "UsdPrim::GetDescendants", "UsdPrim::GetAllDescendants", "UsdPrim::GetFilteredDescendants",
                             "UsdPrim::SiblingRange", "UsdPrim::SubtreeRange", "SiblingRange", "SubtreeRange",
                             "UsdPrimRange", "UsdPrimSiblingRange", "UsdPrimSubtreeRange",
                             "UsdPrimSiblingIterator", "UsdPrimSubtreeIterator", "Usd_PrimFlagsPredicate",
                             "Usd_PrimFlagsConjunction", "Usd_PrimFlagsDisjunction").skip());

        // --- Long tail of Sdf / Usd / Pcp types referenced by methods we do not expose ----------
        infoMap.put(new Info("UsdStagePopulationMask", "UsdStageLoadRules", "UsdMetadataValueMap",
                             "PcpPrimIndex", "PcpVariantFallbackMap", "PcpErrorVector",
                             "SdfLayerHandleVector", "SdfLayerOffsetVector", "SdfBatchNamespaceEdit",
                             "SdfNamespaceEditDetailVector", "SdfRelocates", "SdfLayer::Result",
                             "SdfLayer::RootPrimsView", "SdfSubLayerProxy", "SdfSpecHandle",
                             "SdfAttributeSpecHandle", "SdfRelationshipSpecHandle", "SdfPropertySpecHandleVector",
                             "SdfLayerStateDelegateBasePtr", "SdfLayerStateDelegateBaseRefPtr",
                             "UsdPrimTypeInfo", "Vt_ValueShapeDataAccess", "Vt_ShapeData").skip());

        // UsdObject::GetStage returns UsdStageWeakPtr, which is the same type as UsdStagePtr.
        infoMap.put(new Info("UsdStageWeakPtr").pointerTypes("UsdStagePtr"));
    }
}
