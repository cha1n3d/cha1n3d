package com.bobux.vaz2109.client;

import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.RoleC2S;
import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class RoleScreen extends Screen {
   private static final int W = 380;
   private static final int H = 262;
   private static final int ROW = 29;
   private int selected;
   private Button choose;
   private boolean sent;

   public RoleScreen() {
      super(Component.translatable("gui.vaz2109.role.title"));
   }

   protected void init() {
      int x = (this.width - 380) / 2;
      int y = (this.height - 262) / 2;
      this.choose = (Button)this.addRenderableWidget(Button.builder(Component.empty(), b -> {
         if (!this.sent) {
            this.sent = true;
            Network.CHANNEL.sendToServer(new RoleC2S(0, this.selected));
         }
      }).bounds(x + 140, y + 262 - 28, 232, 20).build());
      this.updateButton();
   }

   private void updateButton() {
      this.choose
         .setMessage(Component.translatable("gui.vaz2109.role.choose", new Object[]{Component.translatable("role.vaz2109." + Role.values()[this.selected].id)}));
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      int x = (this.width - 380) / 2;
      int y = (this.height - 262) / 2;

      for (int i = 0; i < Role.values().length; i++) {
         int ry = y + 24 + i * 29;
         if (mx >= (double)(x + 8) && mx < (double)(x + 132) && my >= (double)ry && my < (double)(ry + 29 - 3)) {
            this.selected = i;
            this.updateButton();
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      int x = (this.width - 380) / 2;
      int y = (this.height - 262) / 2;
      Role sel = Role.values()[this.selected];
      g.fill(x, y, x + 380, y + 262, -535818216);
      g.fill(x, y, x + 380, y + 1, 0xFF000000 | sel.color);
      g.fill(x, y + 262 - 1, x + 380, y + 262, 0xFF000000 | sel.color);
      g.drawCenteredString(this.font, this.title, x + 190, y + 8, 16777215);

      for (int i = 0; i < Role.values().length; i++) {
         Role r = Role.values()[i];
         int ry = y + 24 + i * 29;
         boolean hover = mouseX >= x + 8 && mouseX < x + 132 && mouseY >= ry && mouseY < ry + 29 - 3;
         g.fill(x + 8, ry, x + 132, ry + 29 - 3, i == this.selected ? -1073741824 | r.color & 8355711 : (hover ? 1627389951 : 1090519039));
         if (i == this.selected) {
            g.fill(x + 8, ry, x + 10, ry + 29 - 3, 0xFF000000 | r.color);
         }

         Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(r.icon));
         if (item != null) {
            g.renderItem(new ItemStack(item), x + 13, ry + 5);
         }

         g.drawString(this.font, Component.translatable("role.vaz2109." + r.id), x + 33, ry + 9, 0xFF000000 | r.color);
      }

      int dx = x + 142;
      int dw = 230;
      int dy = y + 24;
      g.pose().pushPose();
      g.pose().translate((float)dx, (float)dy, 0.0F);
      g.pose().scale(1.5F, 1.5F, 1.0F);
      g.drawString(this.font, Component.translatable("role.vaz2109." + sel.id), 0, 0, 0xFF000000 | sel.color);
      g.pose().popPose();
      dy += 18;
      dy = this.text(g, Component.translatable("role.vaz2109." + sel.id + ".desc").withStyle(ChatFormatting.GRAY), dx, dy, dw) + 5;
      g.drawString(this.font, Component.translatable("gui.vaz2109.role.passive").withStyle(ChatFormatting.GOLD), dx, dy, 16777215);
      dy += 10;
      dy = this.text(g, Component.literal("• ").append(Component.translatable("role.vaz2109." + sel.id + ".p1")), dx, dy, dw);
      dy = this.text(g, Component.literal("• ").append(Component.translatable("role.vaz2109." + sel.id + ".p2")), dx, dy, dw) + 5;
      g.drawString(
         this.font,
         Component.translatable("gui.vaz2109.role.abilities", new Object[]{ClientModEvents.CLASS.getTranslatedKeyMessage()}).withStyle(ChatFormatting.AQUA),
         dx,
         dy,
         16777215
      );
      dy += 10;

      for (Ability a : Ability.of(sel)) {
         dy = this.text(
            g,
            Component.literal("• ")
               .append(Component.translatable("ability.vaz2109." + a.id).withStyle(ChatFormatting.WHITE))
               .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
               .append(a.requirement().copy().withStyle(ChatFormatting.GRAY)),
            dx,
            dy,
            dw
         );
      }

      super.render(g, mouseX, mouseY, partialTick);
   }

   private int text(GuiGraphics g, Component text, int x, int y, int width) {
      for (FormattedCharSequence line : this.font.split(text, width)) {
         g.drawString(this.font, line, x, y, 14737632);
         y += 10;
      }

      return y;
   }
}
