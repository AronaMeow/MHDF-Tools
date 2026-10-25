package cn.chengzhimeow.mhdftools.message;

import cn.chengzhimeow.mhdftools.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class MiniMessageUtil {
    /**
     * miniMessage格式化
     *
     * @param message 文本
     * @return 文本实例
     */
    public static TextComponent miniMessage(String message) {
        if (message == null) return new TextComponent();
        return new TextComponent(MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * miniMessage格式化(带标签解析器)
     *
     * @param message   文本
     * @param resolvers 标签解析器
     * @return 文本实例
     */
    public static TextComponent miniMessage(String message, TagResolver... resolvers) {
        if (message == null) return new TextComponent();
        return new TextComponent(MiniMessage.miniMessage().deserialize(message, resolvers));
    }
}
