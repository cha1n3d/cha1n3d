import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Verifies every member reference made by the checked jar resolves (with inheritance) against the runtime class path. */
public class RefCheck {
    static class Info { String sup; String[] itf; Set<String> members = new HashSet<>(); }
    static Map<String, Info> classes = new HashMap<>();

    static void load(File jar) throws IOException {
        try (ZipFile z = new ZipFile(jar)) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class") || en.getName().startsWith("META-INF")) continue;
                ClassReader cr = new ClassReader(z.getInputStream(en).readAllBytes());
                Info info = new Info();
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    String name;
                    public void visit(int v, int a, String n, String s, String sup, String[] itf) { name = n; info.sup = sup; info.itf = itf; }
                    public FieldVisitor visitField(int a, String n, String d, String s, Object v) { info.members.add(n + ":" + d); return null; }
                    public MethodVisitor visitMethod(int a, String n, String d, String s, String[] ex) { info.members.add(n + d); return null; }
                    public void visitEnd() { classes.putIfAbsent(name, info); }
                }, ClassReader.SKIP_CODE);
            }
        }
    }

    static boolean has(String owner, String key) {
        if (owner.startsWith("[")) return true;
        Deque<String> q = new ArrayDeque<>(List.of(owner)); Set<String> seen = new HashSet<>();
        while (!q.isEmpty()) {
            String c = q.poll(); if (!seen.add(c)) continue;
            if (c.startsWith("java/") || c.startsWith("javax/") || c.startsWith("jdk/")) {
                try {
                    Class<?> k = Class.forName(c.replace('/', '.'), false, RefCheck.class.getClassLoader());
                    return true; // trust the JDK
                } catch (Throwable t) { return false; }
            }
            Info i = classes.get(c);
            if (i == null) return false;
            if (i.members.contains(key)) return true;
            if (i.sup != null) q.add(i.sup);
            if (i.itf != null) q.addAll(Arrays.asList(i.itf));
        }
        return false;
    }

    public static void main(String[] a) throws Exception {
        File checked = new File(a[0]);
        for (int i = 1; i < a.length; i++) for (String p : a[i].split(":")) if (!p.isEmpty() && new File(p).isFile()) load(new File(p));
        int bad = 0, total = 0;
        try (ZipFile z = new ZipFile(checked)) {
            List<String[]> refs = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class")) continue;
                String cls = en.getName();
                new ClassReader(z.getInputStream(en).readAllBytes()).accept(new ClassVisitor(Opcodes.ASM9) {
                    public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] ex) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            public void visitFieldInsn(int op, String o, String nm, String ds) { refs.add(new String[]{cls, o, nm + ":" + ds}); }
                            public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) { refs.add(new String[]{cls, o, nm + ds}); }
                        };
                    }
                }, 0);
            }
            Set<String> printed = new HashSet<>();
            for (String[] r : refs) {
                if (!(r[1].startsWith("net/minecraft") || r[1].startsWith("com/mojang") || r[1].startsWith("com/bobux"))) continue;
                total++;
                if (!has(r[1], r[2]) && printed.add(r[1] + " " + r[2])) { bad++; System.out.println("MISSING " + r[1] + "." + r[2] + "   (in " + r[0] + ")"); }
            }
        }
        System.out.println("checked " + total + " refs, missing " + bad);
    }
}
