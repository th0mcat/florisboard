/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.keyboard

/**
 * Controls when the split keyboard layout is active. In split mode the keyboard rows are divided
 * into two halves with a configurable gap in the center, making it easier to reach keys on tablets
 * and foldable devices held in landscape orientation.
 */
enum class SplitKeyboardMode {
    /** Split keyboard is never shown (default for phones). */
    OFF,

    /**
     * Split keyboard is shown automatically when the device is in landscape orientation and the
     * screen width is at least 600 dp, which typically indicates a tablet or a foldable device.
     */
    AUTO,

    /** Split keyboard is always shown whenever the keyboard is in landscape orientation. */
    ALWAYS_ON;
}
