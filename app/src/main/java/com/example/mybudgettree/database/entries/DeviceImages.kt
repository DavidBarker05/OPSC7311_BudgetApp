package com.example.mybudgettree.database.entries

/**
 * The rules for the `imagePaths` map on an expense or income, which holds one local image path per device (keyed by
 * device ID). An image is stored only on the device that took it, so another device can see that one exists but can't
 * open it, and has to be asked whether it wants one of its own
 *
 * A device that was asked and said no gets [DECLINED] as its path. That is the text "N/A. Using a marker also keeps
 * "this device said no" different from "this device was never asked", which is what stops it being asked every time
 */
object DeviceImages {
    /** The path stored for a device that was asked about adding an image and said no */
    const val DECLINED = "N/A"

    /**
     * @param paths The `imagePaths` map
     * @return True if any device has a real image. A device that only said no doesn't count
     */
    fun hasImage(paths: Map<String, String>): Boolean = paths.values.any { isRealPath(it) }

    /**
     * @param paths The `imagePaths` map
     * @param deviceId The device to look up
     * @return The device's image path, or null if it has none or said no
     */
    fun pathFor(paths: Map<String, String>, deviceId: String): String? = paths[deviceId]?.takeIf { isRealPath(it) }

    /**
     * Whether the device should be asked if it wants to add an image: another device has one, and this device has
     * neither added one nor said no already
     *
     * @param paths The `imagePaths` map
     * @param deviceId The device that has the item open
     */
    fun needsDecision(paths: Map<String, String>, deviceId: String): Boolean =
        !paths.containsKey(deviceId) && paths.any { (device, path) -> device != deviceId && isRealPath(path) }

    /** Whether [path] is an actual image path, rather than blank or the [DECLINED] marker */
    fun isRealPath(path: String): Boolean = path.isNotBlank() && path != DECLINED
}
