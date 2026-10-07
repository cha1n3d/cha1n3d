package com.bobux.vaz2109.computer;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** VAZ-OS market: left half sells (slot + inventory), right half is the shop list. */
public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
   private static final int SHOP_X = 178;
   private static final int SHOP_Y = 30;
   private static final int SHOP_W = 144;
   private static final int ROW = 20;
   private static final int ROWS = 9;
   private static final int CASE = -2568008;
   private static final int CASE_DARK = -5462116;
   private static final int SCREEN_TOP = -15854029;
   private static final int SCREEN_BOTTOM = -15259315;
   private static final int TEXT = -3348993;
   private static final int GOLD = -10411;
   private int scroll;
   private Button sell;
   private Button sellSame;

   public ComputerScreen(ComputerMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 330;
      this.imageHeight = 226;
      this.inventoryLabelY = ComputerMenu.INV_Y - 11;
      this.inventoryLabelX = ComputerMenu.INV_X;
   }

   protected void init() {
      super.init();
      this.sell = this.addRenderableWidget(
         Button.builder(Component.translatable("gui.vaz2109.pc.sell"), b -> this.click(ComputerMenu.SELL)).bounds(this.leftPos + 60, this.topPos + 54, 104, 14).build()
      );
      this.sellSame = this.addRenderableWidget(
         Button.builder(Component.translatable("gui.vaz2109.pc.sell_same"), b -> this.click(ComputerMenu.SELL_SAME)).bounds(this.leftPos + 60, this.topPos + 72, 104, 14).build()
      );
   }

   private void click(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
      }
   }

   protected void containerTick() {
      super.containerTick();
      boolean has = !this.menu.selling().isEmpty();
      this.sell.active = has;
      this.sellSame.active = has;
   }

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x - 4, y - 4, x + this.imageWidth + 4, y + this.imageHeight + 4, CASE_DARK);
      g.fill(x - 3, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, CASE);
      g.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, SCREEN_TOP, SCREEN_BOTTOM);
      g.fill(x, y, x + this.imageWidth, y + 14, -14530458);
      g.fill(x + SHOP_X - 6, y + 18, x + SHOP_X - 5, y + this.imageHeight - 6, 1090519039);
      g.fill(x + 8, y + 40, x + 168, y + 92, 553648127);
      g.fill(x + SHOP_X, y + SHOP_Y, x + SHOP_X + SHOP_W, y + SHOP_Y + ROW * ROWS, 553648127);

      for (Slot slot : this.menu.slots) {
         int sx = x + slot.x;
         int sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, slot.index == 0 ? -10066330 : -13421773);
         g.fill(sx, sy, sx + 16, sy + 16, slot.index == 0 ? -14013910 : -15461356);
      }

      List<ComputerShop.Offer> offers = ComputerShop.offers();
      long balance = this.menu.balance();
      int hovered = this.rowAt(mouseX, mouseY);

      for (int i = 0; i < ROWS && i + this.scroll < offers.size(); i++) {
         ComputerShop.Offer o = offers.get(i + this.scroll);
         int ry = y + SHOP_Y + i * ROW;
         if (i + this.scroll == hovered) {
            g.fill(x + SHOP_X, ry, x + SHOP_X + SHOP_W, ry + ROW, 822083583);
         }

         ItemStack stack = o.stack();
         g.renderItem(stack, x + SHOP_X + 2, ry + 2);
         g.renderItemDecorations(this.font, stack, x + SHOP_X + 2, ry + 2);
         String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), SHOP_W - 70);
         g.drawString(this.font, name, x + SHOP_X + 22, ry + 6, TEXT, false);
         String price = o.price + " ₽";
         g.drawString(this.font, price, x + SHOP_X + SHOP_W - 4 - this.font.width(price), ry + 6, balance >= o.price ? GOLD : -43691, false);
      }

      if (offers.size() > ROWS) {
         int track = ROW * ROWS;
         int knob = Math.max(12, track * ROWS / offers.size());
         int top = (track - knob) * this.scroll / Math.max(1, offers.size() - ROWS);
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + track, 553648127);
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y + top, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + top + knob, -3355444);
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.title"), 6, 3, -1, false);
      String money = Component.translatable("gui.vaz2109.pc.balance", new Object[]{this.menu.balance()}).getString();
      g.drawString(this.font, money, this.imageWidth - 6 - this.font.width(money), 3, GOLD, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.sell_title"), 10, 24, TEXT, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.shop_title"), SHOP_X, 18, TEXT, false);
      ItemStack stack = this.menu.selling();
      Component worth = stack.isEmpty()
         ? Component.translatable("gui.vaz2109.pc.drop_here")
         : Component.translatable("gui.vaz2109.pc.worth", new Object[]{(long)ComputerShop.value(stack) * (long)stack.getCount()});
      g.drawString(this.font, worth, 14, 44, stack.isEmpty() ? -7829368 : GOLD, false);
      g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.shop_hint"), SHOP_X, this.imageHeight - 14, -7829368, false);
   }

   private int rowAt(double mx, double my) {
      int rx = (int)mx - this.leftPos - SHOP_X;
      int ry = (int)my - this.topPos - SHOP_Y;
      if (rx >= 0 && rx < SHOP_W && ry >= 0 && ry < ROW * ROWS) {
         int index = ry / ROW + this.scroll;
         return index < ComputerShop.offers().size() ? index : -1;
      } else {
         return -1;
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      int row = this.rowAt(mx, my);
      if (row >= 0 && button == 0) {
         this.click((Screen.hasShiftDown() ? ComputerMenu.BUY_TEN : ComputerMenu.BUY) + row);
         return true;
      } else {
         return super.mouseClicked(mx, my, button);
      }
   }

   public boolean mouseScrolled(double mx, double my, double delta) {
      int max = Math.max(0, ComputerShop.offers().size() - ROWS);
      this.scroll = Mth.clamp(this.scroll - (int)Math.signum(delta), 0, max);
      return true;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      super.render(g, mouseX, mouseY, partialTick);
      int row = this.rowAt(mouseX, mouseY);
      if (row >= 0) {
         g.renderTooltip(this.font, ComputerShop.offers().get(row).stack(), mouseX, mouseY);
      } else {
         this.renderTooltip(g, mouseX, mouseY);
      }
   }
}
