import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Lists methods/fields that differ between two jars, per class: catches overrides the SRG remap missed. */
public class MemberDiff {
    static Map<String, Set<String>> read(String jar) throws IOException {
        Map<String, Set<String>> out = new TreeMap<>();
        try (ZipFile z = new ZipFile(jar)) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class")) continue;
                Set<String> m = new TreeSet<>();
                new ClassReader(z.getInputStream(en).readAllBytes()).accept(new ClassVisitor(Opcodes.ASM9) {
                    public FieldVisitor visitField(int a, String n, String d, String s, Object v) { if ((a & Opcodes.ACC_SYNTHETIC) == 0) m.add(n + ":" + d); return null; }
                    public MethodVisitor visitMethod(int a, String n, String d, String s, String[] ex) {
                        if ((a & Opcodes.ACC_SYNTHETIC) == 0 && !n.startsWith("lambda$")) m.add(n + d);
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
                out.put(en.getName(), m);
            }
        }
        return out;
    }

    public static void main(String[] a) throws Exception {
        Map<String, Set<String>> x = read(a[0]), y = read(a[1]);
        int diffs = 0;
        for (String c : x.keySet()) {
            Set<String> mx = x.get(c), my = y.get(c);
            if (my == null) { System.out.println("only in " + a[0] + ": " + c); diffs++; continue; }
            for (String s : mx) if (!my.contains(s)) { System.out.println("- " + c + " " + s); diffs++; }
            for (String s : my) if (!mx.contains(s)) { System.out.println("+ " + c + " " + s); diffs++; }
        }
        for (String c : y.keySet()) if (!x.containsKey(c)) { System.out.println("only in " + a[1] + ": " + c); diffs++; }
        System.out.println(diffs + " differences");
    }
}
