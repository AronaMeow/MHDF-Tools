package cn.chengzhimeow.mhdftools.config;

import cn.chengzhimeow.ccyaml.manager.AbstractYamlManager;
import cn.chengzhimeow.mhdftools.message.ColorUtil;
import cn.chengzhimeow.mhdftools.plugin.PluginManager;
import cn.chengzhimeow.mhdftools.text.TextComponent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class AbstractLangSetting<T> extends AbstractYamlManager {
    @Getter protected T config;

    public AbstractLangSetting() {
        super(ConfigManager.getInstance().getYamlManager());
    }

    protected TextComponent component(String key) {
        return this.component(key, null);
    }

    protected TextComponent component(String key, TextComponent prefix) {
        return this.filled(this.raw(key), prefix, Map.of());
    }

    /**
     * 配置项的原文(还没有解析成文本组件的 yml 字符串)
     *
     * @param key 配置键
     * @return 原文，缺失时为空串
     */
    protected String raw(@NotNull String key) {
        return super.getData().getString(key, "");
    }

    /**
     * 用原文模板填充 <code>{key}</code> 占位符，之后再解析成文本组件。
     *
     * <p>占位符必须赶在解析之前填。Adventure 的文本替换只改文本节点，改不到
     * {@code <click:run_command:"/tpa accept {player}">} 里的命令值(它是个字符串，而且命令值里也
     * 塞不进文本组件)，解析完再换就会把字面量 <code>{player}</code> 留在命令里：玩家点"接受"
     * 发出去的是 {@code /tpa accept {player}}，{player} 被当成玩家名去查请求。
     * 解析前填充的话，正文、悬浮提示、点击命令里的占位符会一起被换掉。
     *
     * <p>填入的值会当作模板的一部分参与解析，所以只能放纯文本(玩家名、命令名、数字)，
     * 不能放带 <code>&lt;</code>、<code>&amp;</code> 的内容，否则会被当成标签。
     * 需要插入文本组件的占位符请继续用 {@link TextComponent#replace(String, TextComponent)}。
     *
     * @param template     配置项原文，见 {@link #raw(String)}
     * @param prefix       <code>{prefix}</code> 用的前缀
     * @param placeholders 占位符，key 不带花括号，value 是纯文本
     * @return 解析后的文本
     */
    protected TextComponent filled(@Nullable String template, @Nullable TextComponent prefix, @NotNull Map<String, String> placeholders) {
        String text = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }

        return ColorUtil.color(text)
                .replace("{version}", PluginManager.getInstance().version)
                .replace("{prefix}", prefix == null ? new TextComponent() : prefix);
    }
}
