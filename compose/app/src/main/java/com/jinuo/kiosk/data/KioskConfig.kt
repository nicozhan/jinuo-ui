package com.jinuo.kiosk.data

/**
 * Single source of truth for backend-configurable kiosk limits.
 *
 * The `cartItemLimit` value is currently a local default of 3. When the
 * backend config API is wired up, only this field needs to be updated —
 * every cart screen and guard reads from here, so no UI code changes.
 */
object KioskConfig {
    /** Maximum total items (sum of quantities) allowed in one cart order. */
    var cartItemLimit: Int = 3
}
