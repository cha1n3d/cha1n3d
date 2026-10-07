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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * VAZ-OS: the left half sells (slot + inventory); the right half is one of four modes picked in the title bar —
 * the market (shop tabs and daily deals), today's orders, the casino (slots and roulette) and investments.
 */
public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
   private static final String[] MODES = new String[]{"market", "orders", "casino", "invest"};
   private static final int MARKET = 0;
   private static final int ORDERS = 1;
   private static final int CASINO = 2;
   private static final int INVEST = 3;
   private static final String[] TABS = new String[]{"deals", "food", "bar", "resources", "weapons", "misc", "rides", "magic", "bank"};
   private static final int SHOP_X = 178;
   private static final int TAB_Y = 16;
   private static final int TAB = 16;
   private static final int SHOP_Y = 46;
   private static final int SHOP_W = 144;
   private static final int ROW = 20;
   private static final int ROWS = 8;
   private static final int ORDER_ROW = 46;
   private static final int SPIN_TICKS = 24;
   private static final int ROLL_TICKS = 30;
   private static final int CASE = -2568008;
   private static final int CASE_DARK = -5462116;
   private static final int SCREEN_TOP = -15854029;
   private static final int SCREEN_BOTTOM = -15259315;
   private static final int TEXT = -3348993;
   private static final int GOLD = -10411;
   private static final int GREEN = -9379728;
   private static final int RED = -43691;
   private static final int GRAY = -7829368;
   private static final int PANEL = 553648127;
   private final List<ComputerScreen.Hot> hots = new ArrayList<>();
   private int mode = MARKET;
   private int tab = 1;
   private int scroll;
   private int bet = 0;
   private int ticks;
   private int seenSpins = -1;
   private int seenRolls = -1;
   private int spinAt = -1000;
   private int rollAt = -1000;
   private int lastGame = -1;
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

   private void click() {
      if (this.minecraft != null) {
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   protected void containerTick() {
      super.containerTick();
      this.ticks++;
      boolean has = !this.menu.selling().isEmpty();
      this.sell.active = has;
      this.sellSame.active = has;
      if (this.seenSpins < 0) {
         this.seenSpins = this.menu.spins();
         this.seenRolls = this.menu.rolls();
      }

      if (this.menu.spins() != this.seenSpins) {
         this.seenSpins = this.menu.spins();
         this.spinAt = this.ticks;
         this.lastGame = 0;
      }

      if (this.menu.rolls() != this.seenRolls) {
         this.seenRolls = this.menu.rolls();
         this.rollAt = this.ticks;
         this.lastGame = 1;
      }
   }

   // ---- data --------------------------------------------------------------------------------------

   private List<Integer> listed() {
      List<Integer> list = new ArrayList<>();
      if (this.tab == ComputerShop.DEALS) {
         for (int i : ComputerShop.deals(this.menu.day())) {
            list.add(i);
         }
      } else {
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

   private static ItemLike mod(String id) {
      ItemLike it = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vaz2109", id));
      return it == null ? Items.BARRIER : it;
   }

   private static ItemStack tabIcon(int tab) {
      ItemLike like = switch (tab) {
         case 1 -> Items.COOKED_BEEF;
         case 2 -> mod("vodka");
         case 3 -> Items.IRON_INGOT;
         case 4 -> Items.IRON_SWORD;
         case 5 -> mod("computer");
         case 6 -> Items.SADDLE;
         case 7 -> Items.ENCHANTED_BOOK;
         case 8 -> ModRegistry.BANKNOTE_1000.get();
         default -> Items.AIR;
      };
      return new ItemStack(like);
   }

   private static ItemStack symbol(int s) {
      ItemLike like = switch (s) {
         case 0 -> Items.SWEET_BERRIES;
         case 1 -> Items.APPLE;
         case 2 -> Items.BELL;
         case 3 -> Items.EMERALD;
         case 4 -> Items.DIAMOND;
         default -> mod("vaz2109");
      };
      return new ItemStack(like);
   }

   private int minutesToNewDay() {
      return this.minecraft != null && this.minecraft.level != null ? (int)((24000L - this.minecraft.level.getDayTime() % 24000L) / 1200L) + 1 : 0;
   }

   private int betValue() {
      return Casino.BETS[this.bet];
   }

   // ---- hit testing --------------------------------------------------------------------------------

   private record Hot(int x0, int y0, int x1, int y1, Runnable action) {
      boolean contains(double mx, double my) {
         return mx >= (double)this.x0 && mx < (double)this.x1 && my >= (double)this.y0 && my < (double)this.y1;
      }
   }

   private static boolean over(int mouseX, int mouseY, int x0, int y0, int x1, int y1) {
      return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
   }

   /** A flat clickable button, registered for this frame. */
   private void button(GuiGraphics g, int x, int y, int w, int h, Component text, boolean active, boolean selected, int mouseX, int mouseY, Runnable action) {
      boolean hot = active && over(mouseX, mouseY, x, y, x + w, y + h);
      int fill = selected ? 1627389951 : (hot ? 822083583 : (active ? 452984831 : 285212671));
      g.fill(x, y, x + w, y + h, fill);
      g.fill(x, y + h - 1, x + w, y + h, active ? 1090519039 : 285212671);
      g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, active ? -1 : GRAY);
      if (active) {
         this.hots.add(new ComputerScreen.Hot(x, y, x + w, y + h, () -> {
            this.click();
            action.run();
         }));
      }
   }

   private int modeX(int m) {
      int x = 50;

      for (int i = 0; i < m; i++) {
         x += this.font.width(Component.translatable("gui.vaz2109.pc.mode." + MODES[i])) + 14;
      }

      return x;
   }

   private int tabAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - TAB_Y;
      return this.mode == MARKET && rx >= 0 && rx < TAB * TABS.length && ry >= 0 && ry < TAB ? rx / TAB : -1;
   }

   private int rowAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - SHOP_Y;
      if (this.mode == MARKET && rx >= 0 && rx < SHOP_W && ry >= 0 && ry < ROW * ROWS) {
         int index = ry / ROW + this.scroll;
         List<Integer> listed = this.listed();
         return index < listed.size() ? listed.get(index) : -1;
      } else {
         return -1;
      }
   }

   private int orderAt(double mx, double my) {
      int rx = (int)Math.floor(mx) - this.leftPos - SHOP_X;
      int ry = (int)Math.floor(my) - this.topPos - 30;
      if (this.mode == ORDERS && rx >= 0 && rx < SHOP_W && ry >= 0) {
         int index = ry / ORDER_ROW;
         return index < this.orders().size() ? index : -1;
      } else {
         return -1;
      }
   }

   // ---- drawing ------------------------------------------------------------------------------------

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      this.hots.clear();
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x - 4, y - 4, x + this.imageWidth + 4, y + this.imageHeight + 4, CASE_DARK);
      g.fill(x - 3, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, CASE);
      g.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, SCREEN_TOP, SCREEN_BOTTOM);
      g.fill(x, y, x + this.imageWidth, y + 14, -14530458);
      g.fill(x + SHOP_X - 6, y + 18, x + SHOP_X - 5, y + this.imageHeight - 6, 1090519039);
      g.fill(x + 8, y + 40, x + 168, y + 92, PANEL);

      for (int m = 0; m < MODES.length; m++) {
         Component label = Component.translatable("gui.vaz2109.pc.mode." + MODES[m]);
         int mx = x + this.modeX(m);
         int w = this.font.width(label) + 10;
         final int pick = m;
         this.button(g, mx, y + 1, w, 12, label, true, m == this.mode, mouseX, mouseY, () -> {
            this.mode = pick;
            this.scroll = 0;
         });
      }

      for (Slot slot : this.menu.slots) {
         int sx = x + slot.x;
         int sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, slot.index == 0 ? -10066330 : -13421773);
         g.fill(sx, sy, sx + 16, sy + 16, slot.index == 0 ? -14013910 : -15461356);
      }

      switch (this.mode) {
         case ORDERS -> this.renderOrders(g, x, y, mouseX, mouseY);
         case CASINO -> this.renderCasino(g, x + SHOP_X, y, mouseX, mouseY, partialTick);
         case INVEST -> this.renderInvest(g, x + SHOP_X, y, mouseX, mouseY);
         default -> this.renderShop(g, x, y, mouseX, mouseY);
      }
   }

   private void renderShop(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
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
            g.renderItem(tabIcon(t), 0, 0);
            g.pose().popPose();
         }
      }

      List<ComputerShop.Offer> offers = ComputerShop.offers();
      List<Integer> listed = this.listed();
      int day = this.menu.day();
      long balance = this.menu.balance();
      int hovered = this.rowAt(mouseX, mouseY);
      g.fill(x + SHOP_X, y + SHOP_Y, x + SHOP_X + SHOP_W, y + SHOP_Y + ROW * ROWS, PANEL);

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
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + track, PANEL);
         g.fill(x + SHOP_X + SHOP_W + 2, y + SHOP_Y + top, x + SHOP_X + SHOP_W + 4, y + SHOP_Y + top + knob, -3355444);
      }
   }

   private void renderOrders(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
      List<ComputerShop.Order> orders = this.orders();
      int hovered = this.orderAt(mouseX, mouseY);

      for (int i = 0; i < orders.size(); i++) {
         ComputerShop.Order o = orders.get(i);
         int ry = y + 30 + i * ORDER_ROW;
         boolean done = this.orderDone(i);
         int have = this.have(o);
         boolean ready = !done && have >= o.count;
         g.fill(x + SHOP_X, ry, x + SHOP_X + SHOP_W, ry + ORDER_ROW - 4, done ? 285260032 : (ready && i == hovered ? 822083583 : PANEL));
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

   private void renderCasino(GuiGraphics g, int px, int y, int mouseX, int mouseY, float partialTick) {
      long balance = this.menu.balance();
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.casino.bet"), px, y + 20, TEXT, false);

      for (int i = 0; i < Casino.BETS.length; i++) {
         final int pick = i;
         this.button(g, px + 44 + i * 33, y + 18, 30, 12, Component.literal(String.valueOf(Casino.BETS[i])), true, i == this.bet, mouseX, mouseY, () -> this.bet = pick);
      }

      // slots
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.casino.slots"), px, y + 35, GOLD, false);
      float spun = (float)(this.ticks - this.spinAt) + partialTick;
      boolean spinning = spun < (float)SPIN_TICKS;

      for (int r = 0; r < 3; r++) {
         int rx = px + 4 + r * 48;
         int ry = y + 45;
         g.fill(rx - 1, ry - 1, rx + 41, ry + 41, -10066330);
         g.fill(rx, ry, rx + 40, ry + 40, -15461356);
         boolean stopped = !spinning || spun >= (float)(10 + r * 7);
         int s = stopped ? Casino.reel(this.menu.reels(), r) : Math.floorMod(this.ticks * 7 + r * 13, Casino.SYMBOLS);
         g.pose().pushPose();
         g.pose().translate((float)(rx + 4), (float)(ry + 4) + (stopped ? 0.0F : Mth.sin(spun * 2.0F) * 3.0F), 0.0F);
         g.pose().scale(2.0F, 2.0F, 1.0F);
         g.renderItem(symbol(s), 0, 0);
         g.pose().popPose();
      }

      boolean canBet = balance >= (long)this.betValue();
      this.button(
         g, px + 4, y + 90, 136, 13, Component.translatable("gui.vaz2109.pc.casino.spin", new Object[]{this.betValue()}), canBet && !spinning, false, mouseX, mouseY,
         () -> this.send(ComputerMenu.SLOTS, this.betValue())
      );
      // roulette
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.casino.roulette"), px, y + 110, GOLD, false);
      float rolled = (float)(this.ticks - this.rollAt) + partialTick;
      boolean rolling = rolled < (float)ROLL_TICKS;
      int n = rolling ? Math.floorMod(this.ticks * 11 + 5, 37) : this.menu.number();
      int disc = n == 0 ? -14513374 : (Casino.red(n) ? -3407872 : -15132391);
      g.fill(px + 3, y + 119, px + 33, y + 149, -3355444);
      g.fill(px + 4, y + 120, px + 32, y + 148, disc);
      if (this.menu.rolls() > 0 || rolling) {
         g.pose().pushPose();
         g.pose().translate((float)(px + 18), (float)(y + 129), 0.0F);
         g.pose().scale(1.5F, 1.5F, 1.0F);
         g.drawCenteredString(this.font, String.valueOf(n), 0, 0, -1);
         g.pose().popPose();
      }

      boolean canRoll = canBet && !rolling;
      this.button(g, px + 38, y + 120, 64, 13, Component.translatable("gui.vaz2109.pc.casino.red"), canRoll, false, mouseX, mouseY, () -> this.send(ComputerMenu.ROULETTE, this.betValue() * 4 + Casino.RED));
      this.button(g, px + 38, y + 135, 64, 13, Component.translatable("gui.vaz2109.pc.casino.black"), canRoll, false, mouseX, mouseY, () -> this.send(ComputerMenu.ROULETTE, this.betValue() * 4 + Casino.BLACK));
      this.button(g, px + 106, y + 120, 36, 28, Component.translatable("gui.vaz2109.pc.casino.zero"), canRoll, false, mouseX, mouseY, () -> this.send(ComputerMenu.ROULETTE, this.betValue() * 4 + Casino.ZERO));
      // outcome
      if (this.lastGame >= 0 && !spinning && !rolling) {
         long won = this.menu.won();
         Component result = won > 0L ? Component.translatable("gui.vaz2109.pc.casino.won", new Object[]{won}) : Component.translatable("gui.vaz2109.pc.casino.lost");
         g.drawString(this.font, result, px, y + 156, won > 0L ? GREEN : RED, false);
      }

      int ty = y + 170;
      for (String key : new String[]{"gui.vaz2109.pc.casino.pay1", "gui.vaz2109.pc.casino.pay2", "gui.vaz2109.pc.casino.pay3"}) {
         g.drawString(this.font, Component.translatable(key), px, ty, GRAY, false);
         ty += 10;
      }
   }

   private void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
      int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));

      for (int i = 0; i <= steps; i++) {
         float t = steps == 0 ? 0.0F : (float)i / (float)steps;
         int x = Math.round(Mth.lerp(t, (float)x0, (float)x1));
         int y = Math.round(Mth.lerp(t, (float)y0, (float)y1));
         g.fill(x, y, x + 1, y + 1, color);
      }
   }

   private void renderInvest(GuiGraphics g, int px, int y, int mouseX, int mouseY) {
      long balance = this.menu.balance();

      for (int s = 0; s < Invest.STOCKS; s++) {
         final int stock = s;
         int ry = y + 18 + s * 48;
         g.fill(px, ry, px + SHOP_W, ry + 45, PANEL);
         int now = this.menu.price(s, 0);
         int before = this.menu.price(s, 1);
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.stock." + s), px + 3, ry + 3, TEXT, false);
         String price = now + " ₽";
         g.drawString(this.font, price, px + SHOP_W - 3 - this.font.width(price), ry + 3, GOLD, false);
         float change = before > 0 ? (float)(now - before) * 100.0F / (float)before : 0.0F;
         String delta = String.format("%+.1f%%", change);
         int deltaColor = change > 0.05F ? GREEN : (change < -0.05F ? RED : GRAY);
         g.drawString(this.font, delta, px + SHOP_W - 3 - this.font.width(delta), ry + 13, deltaColor, false);
         // sparkline over the last 8 days
         int lo = Integer.MAX_VALUE;
         int hi = 0;

         for (int k = 0; k < Invest.HISTORY; k++) {
            lo = Math.min(lo, this.menu.price(s, k));
            hi = Math.max(hi, this.menu.price(s, k));
         }

         int gx = px + 3;
         int gy = ry + 12;
         int gw = 56;
         int gh = 16;
         g.fill(gx, gy, gx + gw, gy + gh, 285212671);
         int prevX = -1;
         int prevY = -1;

         for (int k = Invest.HISTORY - 1; k >= 0; k--) {
            int cx = gx + (Invest.HISTORY - 1 - k) * (gw - 1) / (Invest.HISTORY - 1);
            int cy = gy + gh - 1 - (hi == lo ? gh / 2 : (this.menu.price(s, k) - lo) * (gh - 1) / (hi - lo));
            if (prevX >= 0) {
               this.line(g, prevX, prevY, cx, cy, deltaColor);
            }

            prevX = cx;
            prevY = cy;
         }

         int shares = this.menu.shares(s);
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.stock.have", new Object[]{shares}), px + 64, ry + 13, TEXT, false);
         g.drawString(this.font, "≈ " + (long)shares * (long)now + " ₽", px + 64, ry + 22, GRAY, false);
         long one = (long)now + Invest.fee((long)now);
         long ten = (long)now * 10L + Invest.fee((long)now * 10L);
         this.button(g, px + 3, ry + 31, 24, 12, Component.literal("+1"), balance >= one, false, mouseX, mouseY, () -> this.send(ComputerMenu.STOCK_BUY, stock * ComputerMenu.STOCK_ARG + 1));
         this.button(g, px + 30, ry + 31, 28, 12, Component.literal("+10"), balance >= ten, false, mouseX, mouseY, () -> this.send(ComputerMenu.STOCK_BUY, stock * ComputerMenu.STOCK_ARG + 10));
         this.button(g, px + 61, ry + 31, 24, 12, Component.literal("−1"), shares > 0, false, mouseX, mouseY, () -> this.send(ComputerMenu.STOCK_SELL, stock * ComputerMenu.STOCK_ARG + 1));
         this.button(
            g, px + 88, ry + 31, 53, 12, Component.translatable("gui.vaz2109.pc.stock.sell_all"), shares > 0, false, mouseX, mouseY, () -> this.send(ComputerMenu.STOCK_SELL, stock * ComputerMenu.STOCK_ARG)
         );
      }

      int dy = y + 164;
      g.fill(px, dy, px + SHOP_W, dy + 32, PANEL);
      long savings = this.menu.savings();
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.deposit", new Object[]{savings}), px + 3, dy + 3, GOLD, false);
      this.button(g, px + 3, dy + 16, 40, 13, Component.literal("+100"), balance >= 100L, false, mouseX, mouseY, () -> this.send(ComputerMenu.DEPOSIT, 100));
      this.button(g, px + 46, dy + 16, 44, 13, Component.literal("+1000"), balance >= 1000L, false, mouseX, mouseY, () -> this.send(ComputerMenu.DEPOSIT, 1000));
      this.button(g, px + 93, dy + 16, 48, 13, Component.translatable("gui.vaz2109.pc.deposit.take"), savings > 0L, false, mouseX, mouseY, () -> this.send(ComputerMenu.WITHDRAW, 0));
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, "VAZ-OS", 6, 3, -1, false);
      g.drawString(this.font, Component.translatable("gui.vaz2109.pc.sell_title"), 10, 24, TEXT, false);
      String money = Component.translatable("gui.vaz2109.pc.balance", new Object[]{this.menu.balance()}).getString();
      g.drawString(this.font, money, 168 - this.font.width(money), 24, GOLD, false);
      if (this.mode == MARKET) {
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.tab." + TABS[this.tab]), SHOP_X, 35, TEXT, false);
      } else if (this.mode == ORDERS) {
         g.drawString(this.font, Component.translatable("gui.vaz2109.pc.mode.orders.title"), SHOP_X, 19, TEXT, false);
      }

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
      String hint = switch (this.mode) {
         case ORDERS -> "gui.vaz2109.pc.order_hint";
         case CASINO -> "gui.vaz2109.pc.casino.hint";
         case INVEST -> "gui.vaz2109.pc.invest_hint";
         default -> this.tab == ComputerShop.DEALS ? "gui.vaz2109.pc.deals_hint" : "gui.vaz2109.pc.shop_hint";
      };
      g.drawString(this.font, Component.translatable(hint), SHOP_X, this.imageHeight - 14, GRAY, false);
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
      if (button == 0) {
         for (ComputerScreen.Hot hot : this.hots) {
            if (hot.contains(mx, my)) {
               hot.action().run();
               return true;
            }
         }

         int tab = this.tabAt(mx, my);
         if (tab >= 0) {
            this.tab = tab;
            this.scroll = 0;
            this.click();
            return true;
         }

         int row = this.rowAt(mx, my);
         if (row >= 0) {
            this.send(Screen.hasShiftDown() ? ComputerMenu.BUY_TEN : ComputerMenu.BUY, row);
            return true;
         }

         int order = this.orderAt(mx, my);
         if (order >= 0) {
            if (!this.orderDone(order)) {
               this.send(ComputerMenu.ORDER, order);
            }

            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean mouseScrolled(double mx, double my, double delta) {
      if (this.mode == MARKET) {
         int max = Math.max(0, this.listed().size() - ROWS);
         this.scroll = Mth.clamp(this.scroll - (int)Math.signum(delta), 0, max);
      }

      return true;
   }
}
