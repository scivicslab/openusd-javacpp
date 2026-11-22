// Telling OpenUSD where its plugins are, from the program.
//
// OpenUSD finds Ar, Sdf, Usd and UsdGeom through plugInfo.json files. The JNI library is unpacked
// into JavaCPP's cache without them, so a program has had to set PXR_PLUGINPATH_NAME before the
// JVM started. A program launched by something that does not set that variable registers the
// directory itself, before it opens or creates a stage.
#pragma once

#include <string>

#include "pxr/pxr.h"
#include "pxr/base/plug/plugin.h"
#include "pxr/base/plug/registry.h"

/**
 * Registers the plugInfo.json files under the directory (for example <prefix>/lib/usd). True when
 * the core "usd" plugin is known afterwards, whether this call or PXR_PLUGINPATH_NAME found it.
 */
inline bool UsdRegisterPluginPath(const std::string& dir) {
    pxr::PlugRegistry& registry = pxr::PlugRegistry::GetInstance();
    if (!dir.empty()) {
        registry.RegisterPlugins(dir);
    }
    return static_cast<bool>(registry.GetPluginWithName("usd"));
}
