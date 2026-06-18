package com.github.josxha.maplibre

import org.maplibre.android.maps.MapLibreMap

/**
 * Helper to extract the native C++ NativeMapView pointer from a MapLibreMap.
 *
 * The MapLibreMap holds a reference to NativeMapView (package-private) via the
 * NativeMap interface. NativeMapView stores the C++ peer pointer as a private
 * `long nativePtr` field. We use reflection to traverse:
 *   MapLibreMap → nativeMapView (NativeMap) → nativePtr (long)
 *
 * This pointer is passed to the movin route-draw engine's C-API so it can
 * query decoded tile geometry and register a custom render layer without
 * going through MethodChannel.
 *
 * Added by the movin/native-handle fork (PR-4b §5.2).
 */
object NativeHandleHelper {
    /**
     * Returns the native C++ NativeMapView* pointer as a Long, or 0 if
     * reflection fails (e.g. obfuscation, API change).
     */
    @JvmStatic
    fun getNativeMapPtr(map: MapLibreMap): Long {
        return try {
            // Step 1: MapLibreMap has a field `nativeMapView` of type NativeMap.
            val nativeMapField = MapLibreMap::class.java.getDeclaredField("nativeMapView")
            nativeMapField.isAccessible = true
            val nativeMap = nativeMapField.get(map) ?: return 0L

            // Step 2: NativeMapView (implements NativeMap) has `long nativePtr`.
            val nativePtrField = nativeMap.javaClass.getDeclaredField("nativePtr")
            nativePtrField.isAccessible = true
            nativePtrField.getLong(nativeMap)
        } catch (e: Exception) {
            // Reflection failed — return 0 so the Dart side falls back to the
            // interim path (nativeExportAvailable remains effectively false).
            0L
        }
    }
}
