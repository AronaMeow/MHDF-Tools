package cn.chengzhimeow.mhdftools.bukkit.module.feature;

import cn.chengzhimeow.ccscheduler.scheduler.CCScheduler;
import cn.chengzhimeow.mhdftools.bukkit.api.MHDFToolsBukkit;
import cn.chengzhimeow.mhdftools.bukkit.module.Module;
import cn.chengzhimeow.mhdftools.bukkit.module.thread.MenuThread;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class Menu implements InventoryHolder {
    private final boolean enable;
    private final Player player;

    public Menu(@NotNull Module module, boolean enable, @NotNull Player player) {
        this.enable = enable;
        this.player = player;
    }

    public Menu(@NotNull Module module, @NotNull Player player) {
        this(module, true, player);
    }

    /**
     * 触发打开菜单事件的时候
     *
     * @param event 触发打开菜单事件
     */
    public void onOpen(InventoryOpenEvent event) {
        if (!this.isEnable()) return;
        this.open(event);
    }

    /**
     * 触发打开菜单事件的时候
     *
     * @param event 触发打开菜单事件
     */
    protected void open(InventoryOpenEvent event) {
    }

    /**
     * 触发点击菜单事件的时候
     *
     * @param event 触发点击菜单事件
     */
    public void onClick(InventoryClickEvent event) {
        if (!this.isEnable()) return;
        this.click(event);
    }

    /**
     * 触发点击菜单事件的时候
     *
     * @param event 触发点击菜单事件
     */
    protected void click(InventoryClickEvent event) {
    }

    /**
     * 触发关闭菜单事件的时候
     *
     * @param event 触发关闭菜单事件
     */
    public void onClose(InventoryCloseEvent event) {
        if (!this.isEnable()) return;
        this.close(event);
    }

    /**
     * 触发关闭菜单事件的时候
     *
     * @param event 触发关闭菜单事件
     */
    protected void close(InventoryCloseEvent event) {
    }

    /**
     * 打开菜单
     * <p>
     * 菜单内容在菜单线程中构建，真正打开容器必须回到玩家所在的区域线程，
     * 否则 Folia 会抛出 "Cannot init menu async" 主线程检查异常，
     * 导致客户端只收到开窗包、收不到内容同步包，GUI 打开后是空的。
     */
    public void openMenu() {
        if (!this.isEnable()) return;

        MenuThread.getInstance().execute(() -> {
            Inventory menu = this.getInventory();

            CCScheduler.getInstance().getEntityScheduler().runTask(
                    MHDFToolsBukkit.getInstance(),
                    this.player,
                    () -> this.player.openInventory(menu)
            );
        });
    }
}
