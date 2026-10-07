package com.bobux.vaz2109.computer;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.network.ComputerC2S;
import com.bobux.vaz2109.network.Network;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** VAZ-OS market: left half sells (slot + inventory), right half has the shop tabs, today's deals and today's orders. */
public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
   private static final String[] TABS = new String[]{"deals", "food", "resources", "weapons", "gear", "rides", "magic", "bank", "orders"};
   private static final int SHOP_X = 178;
   private static final int TAB_Y = 16;
   private static final int TAB = 16;
   private static final int SHOP_Y = 46;
   private static final int SHOP_W = 144;
   private static final int ROW = 20;
   private static final int ROWS = 8;
   private static final int ORDER_ROW = 46;
   private static final int CASE = -2568008;
   private static final int CASE_DARK = -5462116;
   private static final int SCREEN_TOP = -15854029;
   private static final int SCREEN_BOTTOM = -15259315;
   private static final int TEXT = -3348993;
   private static final int GOLD = -10411;
   private static final int GREEN = -9379728;
   private static final int RED = -43691;
   private static final int GRAY = -7829368;
   private int tab = 1;
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
         Button.builder(Component.translatable("gui.vaz2109.pc.sell"), b -> this.send(ComputerMenu.SELL, 0)).bounds(this.leftPos + 60, this.topPos + 54, 104, 14).build()
      );
      this.sellSame = this.addRenderableWidget(
         Button.builder(Component.translatable("gui.vaz2109.pc.sell_same"), b -> this.send(ComputerMenu.SELL_SAME, 0))
            .bounds(this.leftPos + 60, this.topPos + 72, 104, 14)
            .build()
      );
   }

   private void send(int action, int arg) {
      Network.CHANNEL.sendToServer(new ComputerC2S(this.menu.containerId, action, arg));
   }

   protected void containerTick() {
      super.containerTick();
      boolean has = !this.menu.selling().isEmpty();
      this.sell.active = has;
      this.sellSame.active = has;
   }

   // ---- data --------------------------------------------------------------------------------------

   /** Offer indices shown on the current tab. */
   private List<Integer> listed() {
      List<Integer> list = new ArrayList<>();
      if (this.tab == ComputerShop.DEALS) {
         for (int i : ComputerShop.deals(this.menu.day())) {
            list.add(i);
         }
      } else if (this.tab != ComputerShop.ORDERS) {
         List<ComputerShop.Offer> offers = ComputerShop.offers();

         for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).category == this.tab) {
               list.add(i);
            }
         }
      }

      return list;
   }

   private List<ComputerShop.Order> orders() {
      return this.minecraft != null && this.minecraft.player != null ? ComputerShop.orders(this.minecraft.player.getUUID(), this.menu.day()) : List.of();
   }

   private int have(ComputerShop.Order order) {
      return this.minecraft != null && this.minecraft.player != null ? ComputerShop.count(this.minecraft.player, order.item) : 0;
   }

   private boolean orderDone(int i) {
      return (this.menu.ordersDone() & 1 << i) != 0;
   }

   private static ItemStack icon(int tab) {
      ItemLike like = switch (tab) {
         case 1 -> Items.COOKED_BEEF;
         case 2 -> Items.IRON_INGOT;
         case 3 -> mod("ak74");
         case 4 -> mod("tactical_helmet");
         case 5 -> mod("vaz2109");
         case 6 -> Items.ENCHANTED_BOOK;
         case 7 -> ModRegistry.BANKNOTE_1000.get();
         case 8 -> Items.WRITABLE_BOOK;
         default -> Items.AIR;
      };
      return new ItemStack(like == null ? Items.BARRIER : like);
   }

   private static ItemLike mod(String id) {
      return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("vaz2109", id));
   }

   private int minutesToNewDay() {
      return this.minecraft != null && this.minecraft.level != null ? (int)((24000L - this.minecraft.level.getDayTime() % 24000L) / 1200L) + 1 : 0;
   }

   // ---- hit testing --------------------------------------------------------------------------------

   private int tabAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - TAB_Y;
      return rx >= 0 && rx < TAB * TABS.length && ry >= 0 && ry < TAB ? rx / TAB : -1;
   }

   private int rowAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - SHOP_Y;
      if (this.tab != ComputerShop.ORDERS && rx >= 0 && rx < SHOP_W && ry >= 0 && ry < ROW * ROWS) {
         int index = ry / ROW + this.scroll;
         List<Integer> listed = this.listed();
         return index < listed.size() ? listed.get(index) : -1;
      } else {
         return -1;
      }
   }

   private int orderAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - SHOP_Y;
      if (this.tab == ComputerShop.ORDERS && rx >= 0 && rx < SHOP_W && ry >= 0) {
         int index = ry / ORDER_ROW;
         return index < this.orders().size() ? index : -1;
      } else {
         return -1;
      }
   }

   // ---- drawing ------------------------------------------------------------------------------------

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x - 4, y - 4, x + this.imageWidth + 4, y + this.imageHeight + 4, CASE_DARK);
      g.fill(x - 3, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, CASE);
      g.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, SCREEN_TOP, SCREEN_BOTTOM);
      g.fill(x, y, x + this.imageWidth, y + 14, -14530458);
      g.fill(x + SHOP_X - 6, y + 18, x + SHOP_X - 5, y + this.imageHeight - 6, 1090519039);
      g.fill(x + 8, y + 40, x + 168, y + 92, 553648127);

      for (Slot slot : this.menu.slots) {
         int sx = x + slot.x;
         int sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, slot.index == 0 ? -10066330 : -13421773);
         g.fill(sx, sy, sx + 16, sy + 16, slot.index == 0 ? -14013910 : -15461356);
      }

      int hoverTab = this.tabAt(mouseX, mouseY);

      for (int t = 0; t < TABS.length; t++) {
         int tx = x + SHOP_X + t * TAB;
         int color = t == this.tab ? 1627389951 : (t == hoverTab ? 822083583 : 285212671);
         g.fill(tx, y + TAB_Y, tx + TAB - 1, y + TAB_Y + TAB, color);
         if (t == ComputerShop.DEALS) {
            g.drawCenteredString(this.font, "%", tx + 8, y + TAB_Y + 4, RED);
         } else {
            g.pose().pushPose();
            g.pose().translate((float)tx + 1.5F, (float)(y + TAB_Y) + 1.5F, 0.0F);
            g.pose().scale(0.8F, 0.8F, 1.0F);
            g.renderItem(icon(t), 0, 0);
            g.pose().popPose();
         }
      }

      if (this.tab == ComputerShop.ORDERS) {
         this.renderOrders(g, x, y, mouseX, mouseY);
      } else {
         this.renderShop(g, x, y, mouseX, mouseY);
      }
   }

   private void renderShop(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
      List<ComputerShop.Offer> offers = ComputerShop.offers();
      List<Integer> listed = this.listed();
      int day = this.menu.day();
      long balance = this.menu.balance();
      int hovered = this.rowAt(mouseX, mouseY);
      g.fill(x + SHOP_X, y + SHOP_Y, x + SHOP_X + SHOP_W, y + SHOP_Y + ROW * ROWS, 553648127);

      for (int i = 0; i < ROWS && i + this.scroll < listed.size(); i++) {
         int index = listed.get(i + this.scroll);
         ComputerShop.Offer o = offers.get(index);
         int ry = y + SHOP_Y + i * ROW;
         if (index == hovered) {
            g.fill(x + SHOP_X, ry, x + SHOP_X + SHOP_W, ry + ROW, 822083583);
         }

         ItemStack stack = o.stack();
         g.renderItem(stack, x + SHOP_X + 2, ry + 2);
         g.renderItemDecorations(this.font, stack, x + SHOP_X + 2, ry + 2);
         int price = ComputerShop.price(index, day);
         boolean sale = price != o.price;
         String cost = price + " ₽";
         int right = x + SHOP_X + SHOP_W - 4;
         g.drawString(this.font, cost, right - this.font.width(cost), ry + 6, balance >= price ? (sale ? GREEN : GOLD) : RED, false);
         int nameWidth = SHOP_W - 30 - this.font.width(cost);
         if (sale) {
            String tag = "-" + ComputerShop.DEAL_PERCENT + "%";
            int tw = this.font.width(tag);
            g.drawString(this.font, tag, right - this.font.width(cost) - 4 - tw, ry + 6, RED, false);
            nameWidth -= tw + 4;
         }

         g.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), nameWidth), x + SHOP_X + 22, ry + 6, TEXT, false);
      }

      if (listed.size() > ROWS) {
         int track = ROW * ROWS;
         int knob = Math.max(12, track * ROWS / listed.size());
         int top = (track - knob) * this.scroll / Math.max(1, listed.size() - ROWS);
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + track, 553648127);
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y + top, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + top + knob, -3355444);
      }
   }

   private void renderOrders(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
      List<ComputerShop.Order> orders = this.orders();
      int hovered = this.orderAt(mouseX, mouseY);

      for (int i = 0; i < orders.size(); i++) {
         ComputerShop.Order o = orders.get(i);
         int ry = y + SHOP_Y + i * ORDER_ROW;
         boolean done = this.orderDone(i);
         int have = this.have(o);
         boolean ready = !done && have >= o.count;
         g.fill(x + SHOP_X, ry, x + SHOP_X + SHOP_W, ry + ORDER_ROW - 4, done ? 285260032 : (ready && i == hovered ? 822083583 : 553648127));
         ItemStack stack = new ItemStack(o.item);
         g.renderItem(stack, x + SHOP_X + 3, ry + 4);
         String name = this.font.plainSubstrByWidth("×" + o.count + " " + stack.getHoverName().getString(), SHOP_W - 26);
         g.drawString(this.font, name, x + SHOP_X + 22, ry + 4, TEXT, false);
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.order.reward", new Object[]{o.reward}), x + SHOP_X + 22, ry + 15, GOLD, false);
         Component status = done
            ? Component.translatable("gui.vaz2109.pc.order.done")
            : (ready
               ? Component.translatable("gui.vaz2109.pc.order.ready", new Object[]{have, o.count})
               : Component.translatable("gui.vaz2109.pc.order.have", new Object[]{have, o.count}));
         g.drawString(this.font, status, x + SHOP_X + 22, ry + 27, done || ready ? GREEN : GRAY, false);
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.title"), 6, 3, -1, false);
      String money = Component.translatable("gui.vaz2109.pc.balance", new Object[]{this.menu.balance()}).getString();
      g.drawString(this.font, money, this.imageWidth - 6 - this.font.width(money), 3, GOLD, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.sell_title"), 10, 24, TEXT, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.tab." + TABS[this.tab]), SHOP_X, 35, TEXT, false);
      ItemStack stack = this.menu.selling();
      if (stack.isEmpty()) {
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.drop_here"), 14, 44, GRAY, false);
      } else {
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.worth", new Object[]{this.menu.quote()}), 14, 44, GOLD, false);
         int demand = this.menu.demand();
         g.drawString(
            this.font, Component.translatable("gui.vaz2109.pc.demand", new Object[]{demand}), 10, 94, demand >= 80 ? GREEN : (demand >= 50 ? GOLD : RED), false
         );
         if (demand < 100) {
            g.drawString(this.font, Component.translatable("gui.vaz2109.pc.demand_hint"), 10, 104, GRAY, false);
         }
      }

      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.new_day", new Object[]{this.minutesToNewDay()}), 10, 115, GRAY, false);

      g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
      Component hint = this.tab == ComputerShop.ORDERS
         ? Component.translatable("gui.vaz2109.pc.order_hint")
         : (this.tab == ComputerShop.DEALS ? Component.translatable("gui.vaz2109.pc.deals_hint") : Component.translatable("gui.vaz2109.pc.shop_hint"));
      g.drawString(this.font, hint, SHOP_X, this.imageHeight - 14, GRAY, false);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      super.render(g, mouseX, mouseY, partialTick);
      int tab = this.tabAt(mouseX, mouseY);
      int row = this.rowAt(mouseX, mouseY);
      int order = this.orderAt(mouseX, mouseY);
      if (tab >= 0) {
         g.renderTooltip(this.font, Component.translatable("gui.vaz2109.pc.tab." + TABS[tab]), mouseX, mouseY);
      } else if (row >= 0) {
         ComputerShop.Offer o = ComputerShop.offers().get(row);
         ItemStack stack = o.stack();
         List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, stack));
         lines.add(Component.translatable("gui.vaz2109.pc.lot", new Object[]{ComputerShop.price(row, this.menu.day()), o.count}).withStyle(ChatFormatting.GOLD));
         int resale = ComputerShop.value(stack);
         lines.add(
            resale > 0
               ? Component.translatable("gui.vaz2109.pc.resale", new Object[]{resale}).withStyle(ChatFormatting.GRAY)
               : Component.translatable("gui.vaz2109.pc.no_resale").withStyle(ChatFormatting.DARK_GRAY)
         );
         g.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
      } else if (order >= 0) {
         g.renderTooltip(this.font, new ItemStack(this.orders().get(order).item), mouseX, mouseY);
      } else {
         this.renderTooltip(g, mouseX, mouseY);
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      int tab = this.tabAt(mx, my);
      if (tab >= 0 && button == 0) {
         this.tab = tab;
         this.scroll = 0;
         this.click();
         return true;
      }

      int row = this.rowAt(mx, my);
      if (row >= 0 && button == 0) {
         this.send(Screen.hasShiftDown() ? ComputerMenu.BUY_TEN : ComputerMenu.BUY, row);
         return true;
      }

      int order = this.orderAt(mx, my);
      if (order >= 0 && button == 0) {
         if (!this.orderDone(order)) {
            this.send(ComputerMenu.ORDER, order);
         }

         return true;
      }

      return super.mouseClicked(mx, my, button);
   }

   private void click() {
      if (this.minecraft != null) {
         this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   public boolean mouseScrolled(double mx, double my, double delta) {
      int max = Math.max(0, this.listed().size() - ROWS);
      this.scroll = Mth.clamp(this.scroll - (int)Math.signum(delta), 0, max);
      return true;
   }
}
