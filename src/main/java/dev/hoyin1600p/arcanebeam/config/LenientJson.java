package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Per-field tolerance for hand-edited config files. Before Gson maps a section onto its settings class, every
 * value is converted to the type its field expects when that is unambiguous (a colour written as
 * {@code "#RRGGBB"}, {@code "0xRRGGBB"} or an unsigned number above Integer.MAX_VALUE, a boolean written as
 * {@code "true"} or 1, a number written as a string). A value that still cannot be read is removed, so that one
 * field keeps the default of its section while the rest of the section loads. The written format is unchanged:
 * this only rewrites the JSON tree that is about to be read.
 *
 * <p>This is a tree pass rather than a Gson TypeAdapter on purpose: an adapter cannot know the default of the field
 * it is reading, and cannot name the field in its warning.
 */
final class LenientJson {
    private LenientJson() {
    }

    /**
     * Rewrites {@code object} in place for the fields of {@code type}. {@code path} names the section for the one
     * warning each unreadable field produces.
     *
     * @return the number of fields that were dropped back to their default
     */
    static int coerce(JsonObject object, Class<?> type, String path) {
        int dropped = 0;
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            String name = field.getName();
            JsonElement element = object.get(name);
            if (element == null) {
                continue;
            }
            Class<?> fieldType = field.getType();
            JsonElement converted = null;
            boolean handled = true;
            if (fieldType == int.class || fieldType == Integer.class) {
                converted = toInt(element);
            } else if (fieldType == long.class || fieldType == Long.class) {
                converted = toLong(element);
            } else if (fieldType == float.class || fieldType == Float.class
                    || fieldType == double.class || fieldType == Double.class) {
                converted = toDecimal(element);
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                converted = toBoolean(element);
            } else if (fieldType == String.class) {
                converted = element.isJsonPrimitive() ? element : null;
            } else if (fieldType == int[].class) {
                converted = toIntArray(element);
            } else if (isSettingsClass(fieldType)) {
                if (element.isJsonObject()) {
                    dropped += coerce(element.getAsJsonObject(), fieldType, path + "." + name);
                    continue;
                }
            } else {
                handled = false;
            }
            if (!handled) {
                continue;
            }
            if (converted == null) {
                object.remove(name);
                dropped++;
                ArcaneBeam.LOGGER.warn("Arcane Beam config value {}.{} ({}) is not a valid {}; using its default",
                        path, name, abbreviate(element), describe(fieldType));
            } else if (converted != element) {
                object.add(name, converted);
            }
        }
        return dropped;
    }

    static boolean isSettingsClass(Class<?> type) {
        return !type.isPrimitive() && !type.isArray() && !type.isEnum()
                && type.getName().startsWith("dev.hoyin1600p.arcanebeam.config.model.");
    }

    /** An int, with colour notation accepted. Returns the same element when it is already a plain int. */
    static JsonElement toInt(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return numberToInt(primitive.getAsBigDecimal(), element);
        }
        if (!primitive.isString()) {
            return null;
        }
        String text = primitive.getAsString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            if (text.startsWith("#")) {
                String digits = text.substring(1);
                if (digits.length() != 6 && digits.length() != 8) {
                    return null;
                }
                return hexToInt(digits, 6, 8);
            }
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return hexToInt(text.substring(2), 1, 8);
            }
            return numberToInt(new BigDecimal(text), null);
        } catch (NumberFormatException failure) {
            return null;
        }
    }

    private static JsonElement hexToInt(String digits, int minLength, int maxLength) {
        if (digits.length() < minLength || digits.length() > maxLength) {
            throw new NumberFormatException("hex length");
        }
        // Eight digits are #AARRGGBB; the value is the same 32 bits Gson would store after wrapping.
        return new JsonPrimitive((int) Long.parseLong(digits, 16));
    }

    private static JsonElement numberToInt(BigDecimal number, JsonElement original) {
        BigDecimal integral = number.setScale(0, java.math.RoundingMode.HALF_UP);
        if (integral.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                && integral.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) {
            if (original != null && number.compareTo(integral) == 0) {
                return original;
            }
            return new JsonPrimitive(integral.intValueExact());
        }
        if (integral.compareTo(BigDecimal.valueOf(0xFFFFFFFFL)) <= 0 && integral.signum() > 0) {
            // An unsigned 32-bit colour such as 4294967295 wraps to the int with the same bits.
            return new JsonPrimitive((int) integral.longValueExact());
        }
        return null;
    }

    private static JsonElement toLong(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        try {
            BigDecimal number = element.getAsJsonPrimitive().isNumber()
                    ? element.getAsJsonPrimitive().getAsBigDecimal()
                    : new BigDecimal(element.getAsString().trim());
            return new JsonPrimitive(number.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact());
        } catch (NumberFormatException | ArithmeticException failure) {
            return null;
        }
    }

    /** A float or double: a number, or a numeric string. Non-finite text is not accepted. */
    private static JsonElement toDecimal(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return element;
        }
        if (!primitive.isString()) {
            return null;
        }
        try {
            double value = Double.parseDouble(primitive.getAsString().trim());
            return Double.isFinite(value) ? new JsonPrimitive(value) : null;
        } catch (NumberFormatException failure) {
            return null;
        }
    }

    /** A boolean: true/false, the strings "true"/"false" in any case, or the numbers 1 and 0. */
    private static JsonElement toBoolean(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return element;
        }
        if (primitive.isNumber()) {
            double value = primitive.getAsDouble();
            if (value == 1.0D) {
                return new JsonPrimitive(true);
            }
            return value == 0.0D ? new JsonPrimitive(false) : null;
        }
        String text = primitive.getAsString().trim().toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("1")) {
            return new JsonPrimitive(true);
        }
        if (text.equals("false") || text.equals("0")) {
            return new JsonPrimitive(false);
        }
        return null;
    }

    /** An int array; a single value is read as a one-entry array. One unreadable entry rejects the whole array. */
    private static JsonElement toIntArray(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonElement single = toInt(element);
            if (single == null) {
                return null;
            }
            JsonArray array = new JsonArray();
            array.add(single);
            return array;
        }
        if (!element.isJsonArray()) {
            return null;
        }
        JsonArray source = element.getAsJsonArray();
        List<JsonElement> converted = new ArrayList<>(source.size());
        boolean changed = false;
        for (JsonElement entry : source) {
            JsonElement value = toInt(entry);
            if (value == null) {
                return null;
            }
            changed |= value != entry;
            converted.add(value);
        }
        if (!changed) {
            return element;
        }
        JsonArray array = new JsonArray();
        converted.forEach(array::add);
        return array;
    }

    private static String describe(Class<?> type) {
        if (type == int[].class) {
            return "list of whole numbers or colours";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "true/false value";
        }
        if (type == String.class) {
            return "text value";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "whole number or colour";
        }
        return "number";
    }

    private static String abbreviate(JsonElement element) {
        String text = element.toString();
        return text.length() <= 40 ? text : text.substring(0, 37) + "...";
    }
}
