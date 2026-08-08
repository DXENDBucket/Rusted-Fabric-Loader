package io.github.endx.rustedfabric.android.inspector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {
    }

    static String write(Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value, 0);
        output.append('\n');
        return output.toString();
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder output, Object value, int indent) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String) {
            quote(output, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            output.append(value);
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            output.append('{');
            if (!map.isEmpty()) {
                output.append('\n');
                Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    spaces(output, indent + 2);
                    quote(output, entry.getKey());
                    output.append(": ");
                    append(output, entry.getValue(), indent + 2);
                    if (iterator.hasNext()) {
                        output.append(',');
                    }
                    output.append('\n');
                }
                spaces(output, indent);
            }
            output.append('}');
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            output.append('[');
            if (!list.isEmpty()) {
                output.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    spaces(output, indent + 2);
                    append(output, list.get(i), indent + 2);
                    if (i + 1 < list.size()) {
                        output.append(',');
                    }
                    output.append('\n');
                }
                spaces(output, indent);
            }
            output.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void quote(StringBuilder output, String text) {
        output.append('"');
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            switch (value) {
                case '"': output.append("\\\""); break;
                case '\\': output.append("\\\\"); break;
                case '\b': output.append("\\b"); break;
                case '\f': output.append("\\f"); break;
                case '\n': output.append("\\n"); break;
                case '\r': output.append("\\r"); break;
                case '\t': output.append("\\t"); break;
                default:
                    if (value < 0x20) {
                        output.append(String.format("\\u%04x", (int) value));
                    } else {
                        output.append(value);
                    }
            }
        }
        output.append('"');
    }

    private static void spaces(StringBuilder output, int count) {
        for (int i = 0; i < count; i++) {
            output.append(' ');
        }
    }
}
