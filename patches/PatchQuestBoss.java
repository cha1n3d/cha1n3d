import org.objectweb.asm.*;
import java.nio.file.*;

/** Boss bar of QuestBoss: shown to a player only after that player damages the boss. */
public class PatchQuestBoss {
    static final String OWNER = "com/bobux/vaz2109/entity/QuestBoss";
    static final String SBE = "net/minecraft/server/level/ServerBossEvent";
    static final String SP = "net/minecraft/server/level/ServerPlayer";
    static int addRemoved = 0, hookAdded = 0;

    public static void main(String[] a) throws Exception {
        byte[] in = Files.readAllBytes(Path.of(a[0]));
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("m_6457_")) return new StartSeen(mv);
                if (name.equals("m_6469_")) return new Hurt(mv);
                return mv;
            }
        }, 0);
        if (addRemoved != 1 || hookAdded != 1) throw new IllegalStateException("patch mismatch " + addRemoved + " " + hookAdded);
        Files.write(Path.of(a[1]), cw.toByteArray());
        System.out.println("patched OK");
    }

    /** startSeenByPlayer: drop `this.bossEvent.addPlayer(player)`, keep super call. */
    static class StartSeen extends MethodVisitor {
        int state = 0; // 0 = before super call, 1 = after super (skip until addPlayer), 2 = done
        StartSeen(MethodVisitor mv) { super(Opcodes.ASM9, mv); }
        @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
            if (state == 1 && owner.equals(SBE) && name.equals("m_6543_")) { state = 2; addRemoved++; return; }
            super.visitMethodInsn(op, owner, name, desc, itf);
            if (state == 0 && op == Opcodes.INVOKESPECIAL && name.equals("m_6457_")) state = 1;
        }
        @Override public void visitVarInsn(int op, int v) { if (state != 1) super.visitVarInsn(op, v); }
        @Override public void visitFieldInsn(int op, String o, String n, String d) { if (state != 1) super.visitFieldInsn(op, o, n, d); }
    }

    /** hurt: once the quest check passed, reveal the bar to the attacking player (also via projectiles). */
    static class Hurt extends MethodVisitor {
        boolean sawRefuseJump = false, done = false;
        Hurt(MethodVisitor mv) { super(Opcodes.ASM9, mv); }
        @Override public void visitJumpInsn(int op, Label l) { super.visitJumpInsn(op, l); if (op == Opcodes.IFEQ) sawRefuseJump = true; }
        @Override public void visitFrame(int type, int nl, Object[] l, int ns, Object[] s) {
            super.visitFrame(type, nl, l, ns, s);
            if (sawRefuseJump && !done) {
                done = true; hookAdded++;
                Label skip = new Label();
                // Entity e = source.getEntity(); if (e instanceof ServerPlayer) this.bossEvent.addPlayer((ServerPlayer) e);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/damagesource/DamageSource", "m_7639_", "()Lnet/minecraft/world/entity/Entity;", false);
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, SP);
                mv.visitJumpInsn(Opcodes.IFEQ, skip);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, OWNER, "bossEvent", "L" + SBE + ";");
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitTypeInsn(Opcodes.CHECKCAST, SP);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, SBE, "m_6543_", "(L" + SP + ";)V", false);
                mv.visitLabel(skip);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        }
    }
}
