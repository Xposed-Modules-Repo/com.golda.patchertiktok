package com.golda.patchertiktok;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds obfuscated TikTok code by the string constants it uses. String literals survive
 * obfuscation, so this keeps hooks working across TikTok updates without name tables.
 * Dex files are mapped straight from the (stored, uncompressed) APK entries.
 */
final class DexFinder {
    /** A method that loads one of the searched strings. */
    static final class Hit {
        final String className;
        final String methodName;

        Hit(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }

    private final List<ByteBuffer> dexes = new ArrayList<>();

    DexFinder(List<String> apkPaths) {
        for (String path : apkPaths) {
            try {
                mapDexEntries(new File(path));
            } catch (IOException | RuntimeException error) {
                RuntimeLog.log("dex index skipped " + new File(path).getName() + ": " + error.getClass().getSimpleName());
            }
        }
    }

    /** Methods whose code contains a const-string of {@code literal}. */
    List<Hit> methodsUsing(String literal) {
        List<Hit> hits = new ArrayList<>();
        for (ByteBuffer dex : dexes) {
            int index = findString(dex, literal);
            if (index >= 0) scanCode(dex, (unit, position, opcode, start) -> {
                if (opcode == 0x1a) return (dex.getShort(start + position * 2 + 2) & 0xffff) == index;
                return opcode == 0x1b && dex.getInt(start + position * 2 + 2) == index;
            }, hits);
        }
        return hits;
    }

    /** Methods that reference {@code className} via const-class, new-instance, check-cast or instance-of. */
    List<Hit> methodsReferencing(String className) {
        List<Hit> hits = new ArrayList<>();
        String descriptor = "L" + className.replace('.', '/') + ";";
        for (ByteBuffer dex : dexes) {
            int string = findString(dex, descriptor);
            if (string < 0) continue;
            int type = findType(dex, string);
            if (type < 0) continue;
            scanCode(dex, (unit, position, opcode, start) -> {
                if (opcode == 0x1c || opcode == 0x1f || opcode == 0x22) {
                    return (dex.getShort(start + position * 2 + 2) & 0xffff) == type;
                }
                return opcode == 0x20 && (dex.getShort(start + position * 2 + 2) & 0xffff) == type;
            }, hits);
        }
        return hits;
    }

    /** Methods that invoke any method named {@code methodName} declared on {@code className}. */
    List<Hit> methodsCalling(String className, String methodName) {
        List<Hit> hits = new ArrayList<>();
        String descriptor = "L" + className.replace('.', '/') + ";";
        for (ByteBuffer dex : dexes) {
            int string = findString(dex, descriptor);
            int name = findString(dex, methodName);
            if (string < 0 || name < 0) continue;
            int type = findType(dex, string);
            if (type < 0) continue;
            int methodIds = dex.getInt(0x5C);
            scanCode(dex, (unit, position, opcode, start) -> {
                if ((opcode < 0x6e || opcode > 0x72) && (opcode < 0x74 || opcode > 0x78)) return false;
                int target = dex.getShort(start + position * 2 + 2) & 0xffff;
                return (dex.getShort(methodIds + target * 8) & 0xffff) == type
                        && dex.getInt(methodIds + target * 8 + 4) == name;
            }, hits);
        }
        return hits;
    }

    /** "class#method" for every method invoked from {@code className#methodName}. */
    List<String> invokedBy(String className, String methodName) {
        List<String> calls = new ArrayList<>();
        String descriptor = "L" + className.replace('.', '/') + ";";
        for (ByteBuffer dex : dexes) {
            int string = findString(dex, descriptor);
            if (string < 0) continue;
            int type = findType(dex, string);
            if (type < 0) continue;
            int methodIds = dex.getInt(0x5C);
            forEachMethod(dex, type, (definition, methodIndex, code) -> {
                if (!methodName.equals(string(dex, dex.getInt(methodIds + methodIndex * 8 + 4)))) return;
                walk(dex, code, (unit, position, opcode, start) -> {
                    if ((opcode >= 0x6e && opcode <= 0x72) || (opcode >= 0x74 && opcode <= 0x78)) {
                        int target = dex.getShort(start + position * 2 + 2) & 0xffff;
                        int owner = dex.getShort(methodIds + target * 8) & 0xffff;
                        calls.add(typeName(dex, owner) + "#" + string(dex, dex.getInt(methodIds + target * 8 + 4)));
                    }
                    return false;
                });
            });
        }
        return calls;
    }

    Set<String> classesUsing(String literal) {
        Set<String> result = new LinkedHashSet<>();
        for (Hit hit : methodsUsing(literal)) result.add(hit.className);
        return result;
    }

    // ---- APK ------------------------------------------------------------------------------

    private void mapDexEntries(File apk) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(apk, "r"); FileChannel channel = file.getChannel()) {
            long length = channel.size();
            int tail = (int) Math.min(length, 65_557);
            ByteBuffer end = ByteBuffer.allocate(tail).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(end, length - tail);
            int eocd = -1;
            for (int position = tail - 22; position >= 0; position--) {
                if (end.getInt(position) == 0x06054b50) { eocd = position; break; }
            }
            if (eocd < 0) throw new IOException("No zip directory");
            int entries = end.getShort(eocd + 10) & 0xffff;
            long directoryOffset = end.getInt(eocd + 16) & 0xffffffffL;
            long directorySize = end.getInt(eocd + 12) & 0xffffffffL;
            ByteBuffer directory = ByteBuffer.allocate((int) directorySize).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(directory, directoryOffset);
            int position = 0;
            for (int entry = 0; entry < entries; entry++) {
                if (directory.getInt(position) != 0x02014b50) break;
                int method = directory.getShort(position + 10) & 0xffff;
                long size = directory.getInt(position + 24) & 0xffffffffL;
                int nameLength = directory.getShort(position + 28) & 0xffff;
                int extraLength = directory.getShort(position + 30) & 0xffff;
                int commentLength = directory.getShort(position + 32) & 0xffff;
                long localOffset = directory.getInt(position + 42) & 0xffffffffL;
                byte[] nameBytes = new byte[nameLength];
                for (int index = 0; index < nameLength; index++) nameBytes[index] = directory.get(position + 46 + index);
                String name = new String(nameBytes, StandardCharsets.UTF_8);
                position += 46 + nameLength + extraLength + commentLength;
                if (method != 0 || !name.startsWith("classes") || !name.endsWith(".dex")) continue;
                ByteBuffer local = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
                channel.read(local, localOffset);
                long data = localOffset + 30 + (local.getShort(26) & 0xffff) + (local.getShort(28) & 0xffff);
                MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, data, size);
                mapped.order(ByteOrder.LITTLE_ENDIAN);
                dexes.add(mapped);
            }
        }
    }

    // ---- strings --------------------------------------------------------------------------

    private static int findString(ByteBuffer dex, String literal) {
        int count = dex.getInt(0x38);
        int ids = dex.getInt(0x3C);
        int low = 0;
        int high = count - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compare = compare(dex, dex.getInt(ids + middle * 4), literal);
            if (compare == 0) return middle;
            if (compare < 0) low = middle + 1;
            else high = middle - 1;
        }
        return -1;
    }

    /** Compares a MUTF-8 dex string with {@code value} by UTF-16 code units, as dex sorts them. */
    private static int compare(ByteBuffer dex, int offset, String value) {
        int position = offset;
        while ((dex.get(position++) & 0x80) != 0) { /* skip uleb128 length */ }
        int index = 0;
        while (true) {
            int a = dex.get(position) & 0xff;
            if (a == 0) return index == value.length() ? 0 : -1;
            char decoded;
            if (a < 0x80) {
                decoded = (char) a;
                position++;
            } else if ((a & 0xe0) == 0xc0) {
                decoded = (char) (((a & 0x1f) << 6) | (dex.get(position + 1) & 0x3f));
                position += 2;
            } else {
                decoded = (char) (((a & 0x0f) << 12) | ((dex.get(position + 1) & 0x3f) << 6) | (dex.get(position + 2) & 0x3f));
                position += 3;
            }
            if (index == value.length()) return 1;
            char expected = value.charAt(index++);
            if (decoded != expected) return decoded < expected ? -1 : 1;
        }
    }

    private static String string(ByteBuffer dex, int index) {
        int offset = dex.getInt(dex.getInt(0x3C) + index * 4);
        int position = offset;
        while ((dex.get(position++) & 0x80) != 0) { /* skip uleb128 length */ }
        StringBuilder builder = new StringBuilder();
        while (true) {
            int a = dex.get(position) & 0xff;
            if (a == 0) return builder.toString();
            if (a < 0x80) {
                builder.append((char) a);
                position++;
            } else if ((a & 0xe0) == 0xc0) {
                builder.append((char) (((a & 0x1f) << 6) | (dex.get(position + 1) & 0x3f)));
                position += 2;
            } else {
                builder.append((char) (((a & 0x0f) << 12) | ((dex.get(position + 1) & 0x3f) << 6)
                        | (dex.get(position + 2) & 0x3f)));
                position += 3;
            }
        }
    }

    private static String typeName(ByteBuffer dex, int typeIndex) {
        String descriptor = string(dex, dex.getInt(dex.getInt(0x44) + typeIndex * 4));
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    // ---- code -----------------------------------------------------------------------------

    private interface Matcher { boolean match(int unit, int position, int opcode, int start); }

    private interface MethodVisitor { void visit(int definition, int methodIndex, int code); }

    private static void scanCode(ByteBuffer dex, Matcher matcher, List<Hit> hits) {
        int methodIds = dex.getInt(0x5C);
        forEachMethod(dex, -1, (definition, methodIndex, code) -> {
            if (!walk(dex, code, matcher)) return;
            int nameIndex = dex.getInt(methodIds + methodIndex * 8 + 4);
            hits.add(new Hit(typeName(dex, dex.getInt(definition)), string(dex, nameIndex)));
        });
    }

    /** Visits every method with code, optionally only those of the class with type index {@code onlyType}. */
    private static void forEachMethod(ByteBuffer dex, int onlyType, MethodVisitor visitor) {
        int classCount = dex.getInt(0x60);
        int classDefs = dex.getInt(0x64);
        int[] cursor = new int[1];
        for (int c = 0; c < classCount; c++) {
            int definition = classDefs + c * 32;
            if (onlyType >= 0 && dex.getInt(definition) != onlyType) continue;
            int classData = dex.getInt(definition + 24);
            if (classData == 0) continue;
            cursor[0] = classData;
            int staticFields = uleb(dex, cursor);
            int instanceFields = uleb(dex, cursor);
            int directMethods = uleb(dex, cursor);
            int virtualMethods = uleb(dex, cursor);
            for (int field = 0; field < staticFields + instanceFields; field++) {
                uleb(dex, cursor);
                uleb(dex, cursor);
            }
            for (int group = 0; group < 2; group++) {
                int methodIndex = 0;
                int methods = group == 0 ? directMethods : virtualMethods;
                for (int method = 0; method < methods; method++) {
                    methodIndex += uleb(dex, cursor);
                    uleb(dex, cursor);
                    int code = uleb(dex, cursor);
                    if (code != 0) visitor.visit(definition, methodIndex, code);
                }
            }
        }
    }

    /** Walks the instructions of a code item; returns true as soon as {@code matcher} matches. */
    private static boolean walk(ByteBuffer dex, int code, Matcher matcher) {
        int units = dex.getInt(code + 12);
        int start = code + 16;
        int position = 0;
        while (position < units) {
            int unit = dex.getShort(start + position * 2) & 0xffff;
            int opcode = unit & 0xff;
            if (opcode == 0x00 && unit != 0) {
                // Switch and array payloads are data, not instructions.
                if (unit == 0x0100) {
                    position += (dex.getShort(start + position * 2 + 2) & 0xffff) * 2 + 4;
                } else if (unit == 0x0200) {
                    position += (dex.getShort(start + position * 2 + 2) & 0xffff) * 4 + 2;
                } else if (unit == 0x0300) {
                    int width = dex.getShort(start + position * 2 + 2) & 0xffff;
                    long size = dex.getInt(start + position * 2 + 4) & 0xffffffffL;
                    position += (int) ((size * width + 1) / 2) + 4;
                } else {
                    position++;
                }
                continue;
            }
            if (matcher.match(unit, position, opcode, start)) return true;
            position += WIDTH[opcode];
        }
        return false;
    }

    /** type_ids are sorted by string index, so the type for a descriptor is a binary search away. */
    private static int findType(ByteBuffer dex, int stringIndex) {
        int count = dex.getInt(0x40);
        int ids = dex.getInt(0x44);
        int low = 0;
        int high = count - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int value = dex.getInt(ids + middle * 4);
            if (value == stringIndex) return middle;
            if (value < stringIndex) low = middle + 1;
            else high = middle - 1;
        }
        return -1;
    }

    private static int uleb(ByteBuffer dex, int[] cursor) {
        int result = 0;
        int shift = 0;
        int value;
        do {
            value = dex.get(cursor[0]++) & 0xff;
            result |= (value & 0x7f) << shift;
            shift += 7;
        } while ((value & 0x80) != 0);
        return result;
    }

    /** Instruction widths in 16-bit code units, indexed by opcode. */
    private static final int[] WIDTH = new int[256];

    static {
        java.util.Arrays.fill(WIDTH, 1);
        int[][] ranges = {
                {0x02, 0x02, 2}, {0x03, 0x03, 3}, {0x05, 0x05, 2}, {0x06, 0x06, 3}, {0x08, 0x08, 2},
                {0x09, 0x09, 3}, {0x13, 0x13, 2}, {0x14, 0x14, 3}, {0x15, 0x16, 2}, {0x17, 0x17, 3},
                {0x18, 0x18, 5}, {0x19, 0x19, 2}, {0x1a, 0x1a, 2}, {0x1b, 0x1b, 3}, {0x1c, 0x1c, 2},
                {0x1f, 0x20, 2}, {0x22, 0x23, 2}, {0x24, 0x26, 3}, {0x29, 0x29, 2}, {0x2a, 0x2c, 3},
                {0x2d, 0x31, 2}, {0x32, 0x3d, 2}, {0x44, 0x6d, 2}, {0x6e, 0x72, 3}, {0x74, 0x78, 3},
                {0x90, 0xaf, 2}, {0xd0, 0xe2, 2}, {0xfa, 0xfb, 4}, {0xfc, 0xfd, 3}, {0xfe, 0xff, 2},
        };
        for (int[] range : ranges) {
            for (int opcode = range[0]; opcode <= range[1]; opcode++) WIDTH[opcode] = range[2];
        }
    }
}
