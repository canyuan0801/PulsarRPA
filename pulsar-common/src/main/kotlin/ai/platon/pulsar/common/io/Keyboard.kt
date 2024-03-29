package ai.platon.pulsar.common.io

data class KeyDefinition(
    /**
     * The key name, like "Enter", "KeyA", "Digit3", ...
     * */
    val key: String,
    /**
     * The key code, like 13, 65, 51, ...
     * */
    val keyCode: Int,
    /**
     * The key code without location, like 13, 65, 51, ...
     * */
    val keyCodeWithoutLocation: Int? = null,
    /**
     * The key name of the shifted key, like "KeyA", "KeyB", ...
     * */
    val shiftKey: String? = null,
    /**
     * The key code of the shifted key, like 65, 66, ...
     * */
    val shiftKeyCode: Int? = null,
    /**
     * The text of the key, like "Enter", "a", "3", ...
     * */
    val text: String? = null,
    /**
     * The location of the key, like 0, 1, 2, 3, ...
     * */
    val location: Int? = null
)

data class VKeyDescription(
    var key: String,
    var keyCodeWithoutLocation: Int,
    var code: String,
    var location: Int = 0,
    var keyCode: Int,
    var text: String,
    var shifted: VKeyDescription? = null,
) {
    val isModifier: Boolean
        get() = key in KeyboardDescription.KEYBOARD_MODIFIERS
}

typealias KeyboardLayout = Map<String, KeyDefinition>

enum class KeyboardModifier {
    Alt, Control, Meta, Shift
}

val USKeypadLocation = 3

val USKeyboardLayout: KeyboardLayout = mapOf(
    // Functions row
    "Escape" to KeyDefinition("Escape", 27),
    "F1" to KeyDefinition("F1", 112),
    "F2" to KeyDefinition("F2", 113),
    "F3" to KeyDefinition("F3", 114),
    "F4" to KeyDefinition("F4", 115),
    "F5" to KeyDefinition("F5", 116),
    "F6" to KeyDefinition("F6", 117),
    "F7" to KeyDefinition("F7", 118),
    "F8" to KeyDefinition("F8", 119),
    "F9" to KeyDefinition("F9", 120),
    "F10" to KeyDefinition("F10", 121),
    "F11" to KeyDefinition("F11", 122),
    "F12" to KeyDefinition("F12", 123),
    
    // Numbers row
    "Backquote" to KeyDefinition("`", 192, shiftKey = "~"),
    "Digit1" to KeyDefinition("1", 49, shiftKey = "!"),
    "Digit2" to KeyDefinition("2", 50, shiftKey = "@"),
    "Digit3" to KeyDefinition("3", 51, shiftKey = "#"),
    "Digit4" to KeyDefinition("4", 52, shiftKey = "$"),
    "Digit5" to KeyDefinition("5", 53, shiftKey = "%"),
    "Digit6" to KeyDefinition("6", 54, shiftKey = "^"),
    "Digit7" to KeyDefinition("7", 55, shiftKey = "&"),
    "Digit8" to KeyDefinition("8", 56, shiftKey = "*"),
    "Digit9" to KeyDefinition("9", 57, shiftKey = "("),
    "Digit0" to KeyDefinition("0", 48, shiftKey = ")"),
    "Minus" to KeyDefinition("-", 189, shiftKey = "_"),
    "Equal" to KeyDefinition("=", 187, shiftKey = "+"),
    "Backslash" to KeyDefinition("\\", 220, shiftKey = "|"),
    "Backspace" to KeyDefinition("Backspace", 8),
    
    // First row
    "Tab" to KeyDefinition("Tab", 9),
    "KeyQ" to KeyDefinition("q", 81, shiftKey = "Q"),
    "KeyW" to KeyDefinition("w", 87, shiftKey = "W"),
    "KeyE" to KeyDefinition("e", 69, shiftKey = "E"),
    "KeyR" to KeyDefinition("r", 82, shiftKey = "R"),
    "KeyT" to KeyDefinition("t", 84, shiftKey = "T"),
    "KeyY" to KeyDefinition("y", 89, shiftKey = "Y"),
    "KeyU" to KeyDefinition("u", 85, shiftKey = "U"),
    "KeyI" to KeyDefinition("i", 73, shiftKey = "I"),
    "KeyO" to KeyDefinition("o", 79, shiftKey = "O"),
    "KeyP" to KeyDefinition("p", 80, shiftKey = "P"),
    "BracketLeft" to KeyDefinition("[", 219, shiftKey = "{"),
    "BracketRight" to KeyDefinition("]", 221, shiftKey = "}"),
    
    // Second row
    "CapsLock" to KeyDefinition("CapsLock", 20),
    "KeyA" to KeyDefinition("a", 65, shiftKey = "A"),
    "KeyS" to KeyDefinition("s", 83, shiftKey = "S"),
    "KeyD" to KeyDefinition("d", 68, shiftKey = "D"),
    "KeyF" to KeyDefinition("f", 70, shiftKey = "F"),
    "KeyG" to KeyDefinition("g", 71, shiftKey = "G"),
    "KeyH" to KeyDefinition("h", 72, shiftKey = "H"),
    "KeyJ" to KeyDefinition("j", 74, shiftKey = "J"),
    "KeyK" to KeyDefinition("k", 75, shiftKey = "K"),
    "KeyL" to KeyDefinition("l", 76, shiftKey = "L"),
    "Semicolon" to KeyDefinition(";", 186, shiftKey = ":"),
    "Quote" to KeyDefinition("'", 222, shiftKey = "\""),
    "Enter" to KeyDefinition("Enter", 13, text = "\r"),
    
    // Third row
    "ShiftLeft" to KeyDefinition("Shift", 160, keyCodeWithoutLocation = 16, location = 1),
    "KeyZ" to KeyDefinition("z", 90, shiftKey = "Z"),
    "KeyX" to KeyDefinition("x", 88, shiftKey = "X"),
    "KeyC" to KeyDefinition("c", 67, shiftKey = "C"),
    "KeyV" to KeyDefinition("v", 86, shiftKey = "V"),
    "KeyB" to KeyDefinition("b", 66, shiftKey = "B"),
    "KeyN" to KeyDefinition("n", 78, shiftKey = "N"),
    "KeyM" to KeyDefinition("m", 77, shiftKey = "M"),
    "Comma" to KeyDefinition(",", 188, shiftKey = "<"),
    "Period" to KeyDefinition(".", 190, shiftKey = ">"),
    "Slash" to KeyDefinition("/", 191, shiftKey = "?"),
    "ShiftRight" to KeyDefinition("Shift", 161, keyCodeWithoutLocation = 16, location = 2),
    
    // Last row
    "ControlLeft" to KeyDefinition("Control", 162, keyCodeWithoutLocation = 17, location = 1),
    "MetaLeft" to KeyDefinition("Meta", 91, location = 1),
    "AltLeft" to KeyDefinition("Alt", 164, keyCodeWithoutLocation = 18, location = 1),
    "Space" to KeyDefinition(" ", 32),
    "AltRight" to KeyDefinition("Alt", 165, keyCodeWithoutLocation = 18, location = 2),
    "AltGraph" to KeyDefinition("AltGraph", 225),
    "MetaRight" to KeyDefinition("Meta", 92, location = 2),
    "ContextMenu" to KeyDefinition("ContextMenu", 93),
    "ControlRight" to KeyDefinition("Control", 163, keyCodeWithoutLocation = 17, location = 2),
    
    // Center block
    "PrintScreen" to KeyDefinition("PrintScreen", 44),
    "ScrollLock" to KeyDefinition("ScrollLock", 145),
    "Pause" to KeyDefinition("Pause", 19),
    
    "PageUp" to KeyDefinition("PageUp", 33),
    "PageDown" to KeyDefinition("PageDown", 34),
    "Insert" to KeyDefinition("Insert", 45),
    "Delete" to KeyDefinition("Delete", 46),
    "Home" to KeyDefinition("Home", 36),
    "End" to KeyDefinition("End", 35),
    
    // Arrow keys
    "ArrowLeft" to KeyDefinition("ArrowLeft", 37),
    "ArrowUp" to KeyDefinition("ArrowUp", 38),
    "ArrowRight" to KeyDefinition("ArrowRight", 39),
    "ArrowDown" to KeyDefinition("ArrowDown", 40),
    
    // Numpad
    "NumLock" to KeyDefinition("NumLock", 144),
    "NumpadDivide" to KeyDefinition("/", 111, location = 3),
    "NumpadMultiply" to KeyDefinition("*", 106, location = 3),
    "NumpadSubtract" to KeyDefinition("-", 109, location = 3),
    "NumpadAdd" to KeyDefinition("+", 107, location = 3),
    "Numpad1" to KeyDefinition("1", 35, shiftKeyCode = 97, location = 3),
    "Numpad2" to KeyDefinition("2", 40, shiftKeyCode = 98, location = 3),
    "Numpad3" to KeyDefinition("3", 34, shiftKeyCode = 99, location = 3),
    "Numpad4" to KeyDefinition("4", 37, shiftKeyCode = 100, location = 3),
    "Numpad5" to KeyDefinition("5", 12, shiftKeyCode = 101, location = 3),
    "Numpad6" to KeyDefinition("6", 39, shiftKeyCode = 102, location = 3),
    "Numpad7" to KeyDefinition("7", 36, shiftKeyCode = 103, location = 3),
    "Numpad8" to KeyDefinition("8", 38, shiftKeyCode = 104, location = 3),
    "Numpad9" to KeyDefinition("9", 33, shiftKeyCode = 105, location = 3),
    "Numpad0" to KeyDefinition("0", 45, shiftKeyCode = 96, location = 3),
    "NumpadDecimal" to KeyDefinition(".", 46, shiftKeyCode = 110, location = 3),
    "NumpadEnter" to KeyDefinition("Enter", 13, text = "\r", location = 3)
)

object KeyboardDescription {
    
    val KEY_CODE_ALIASES = mapOf(
        "ShiftLeft" to listOf("Shift"),
        "ControlLeft" to listOf("Control"),
        "AltLeft" to listOf("Alt"),
        "MetaLeft" to listOf("Meta"),
        "Enter" to listOf("\n", "\r")
    )
    
    val KEYBOARD_MODIFIERS = KeyboardModifier.entries.map { it.name }
    
    val KEYPAD_LOCATION = USKeypadLocation

    val KEYBOARD_LAYOUT = buildExtendedKeyboardLayoutMapping(USKeyboardLayout)

    /**
     * Build a closure that maps from key codes and key texts to key descriptions.
     * The keys of the map like the following:
     * Key code: "Digit3", "KeyA", "Enter", ...
     * Key text: "3", "a", "\r", ...
     * Key code aliases: "\n", "ShiftLeft", ...
     * */
    private fun buildExtendedKeyboardLayoutMapping(layout: KeyboardLayout): Map<String, VKeyDescription> {
        val result = mutableMapOf<String, VKeyDescription>()
        // The key code: KeyA, KeyB, Enter, ...
        for ((code, definition) in layout) {
            val description = VKeyDescription(
                key = definition.key,
                keyCode = definition.keyCode,
                keyCodeWithoutLocation = definition.keyCodeWithoutLocation ?: definition.keyCode,
                code = code,
                text = definition.text ?: definition.key,
                location = definition.location ?: 0
            )
            if (definition.key.length == 1) {
                description.text = description.key
            }

            val shiftKey = definition.shiftKey
            val shiftedDescription = if (shiftKey != null) {
                description.copy(
                    key = shiftKey,
                    text = shiftKey,
                    keyCode = definition.shiftKeyCode ?: definition.keyCode,
                    shifted = null
                )
            } else null

            // Map from code: Digit3 -> { ... description, shifted }
            result[code] = description.copy(shifted = shiftedDescription)

            // Map from aliases: Shift -> non-shiftable definition
            KEY_CODE_ALIASES[code]?.forEach { alias ->
                result[alias] = description
            }

            // Do not use numpad when converting keys to codes.
            if (definition.location != null) {
                continue
            }

            // Map from key, no shifted
            if (description.key.length == 1) {
                result[description.key] = description
            }

            // Map from shiftKey, no shifted
            shiftedDescription?.let { shifted ->
                result[shifted.key] = shifted.copy(shifted = null)
            }
        }

        return result
    }
}
