package frtest;

import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.technique.impl.Domains;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import com.fallenroses.entity.DomainEntity;
import net.minecraft.world.phys.Vec3;

final class Scenarios {
    /** Flat world surface. */
    static final double Y = -60.0;

    static FrTest.Script build(String name) {
        FrTest.Script s = new FrTest.Script();
        s.server(5, sp -> FrTest.prepare(sp, PlayerClass.SORCERER));
        s.tp(10, 0.5, Y, 0.5, 0.0F, 10.0F);
        if (name.startsWith("domains")) {
            String only = name.contains(":") ? name.substring(name.indexOf(':') + 1) : "0123456789";
            int base = 60;

            for (char ch : only.toCharArray()) {
                domain(s, base, ch - '0', name.startsWith("domainsfull") || only.length() == 1);
                base += name.startsWith("domainsfull") || only.length() == 1 ? 270 : 120;
            }
        }

        return s;
    }

    /** One domain: hand sign, forming seen from outside, the interior both ways, and (full) the collapse. */
    private static void domain(FrTest.Script s, int t, int type, boolean full) {
        String n = "d" + type + "_";
        s.tp(t - 6, 0.5, Y, 0.5, 0.0F, 10.0F);
        s.server(t, sp -> {
            Domains.expand(sp, PlayerDataCap.get(sp), type, 1.0F);
            for (int i = 0; i < 3; i++) {
                Mob m = EntityType.VILLAGER.create(sp.serverLevel());
                m.moveTo(-3.0 + i * 3.0, Y, 7.0, 180.0F, 0.0F);
                m.setNoAi(true);
                sp.serverLevel().addFreshEntity(m);
            }
        });
        s.shot(t + 8, n + "a_sign");
        s.tp(t + 22, 0.5, Y + 6.0, -34.0, 0.0F, 6.0F);
        s.shot(t + 32, n + "b_forming_out");
        s.shot(t + 46, n + "c_formed_out");
        s.tp(t + 50, 0.5, Y, 0.5, 0.0F, -8.0F);
        s.shot(t + 62, n + "d_inside_front");
        s.tp(t + 66, 0.5, Y, 0.5, 180.0F, -8.0F);
        s.shot(t + 78, n + "e_inside_back");
        if (full) {
            s.tp(t + 200, 0.5, Y + 6.0, -34.0, 0.0F, 6.0F);
            s.shot(t + 230, n + "f_cracks");
            s.shot(t + 236, n + "g_shards");
            s.shot(t + 242, n + "h_after");
        } else {
            s.server(t + 90, sp -> sp.serverLevel().getEntities(EntityTypeTest.forClass(DomainEntity.class), d -> true).forEach(d -> d.endIn(15)));
        }

        s.server(t + (full ? 260 : 115), sp -> sp.serverLevel().getEntities(EntityTypeTest.forClass(Mob.class), m -> true).forEach(Mob::discard));
    }

    private Scenarios() {
    }

    static Vec3 at(double x, double z) {
        return new Vec3(x, Y, z);
    }
}
