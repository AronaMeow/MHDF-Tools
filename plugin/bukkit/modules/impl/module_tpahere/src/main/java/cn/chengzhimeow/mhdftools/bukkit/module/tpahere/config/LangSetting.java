package cn.chengzhimeow.mhdftools.bukkit.module.tpahere.config;

import cn.chengzhimeow.mhdftools.bukkit.module.tpahere.ModuleMain;
import cn.chengzhimeow.mhdftools.config.AbstractLangSetting;
import cn.chengzhimeow.mhdftools.config.impl.GlobalLangSetting;
import cn.chengzhimeow.mhdftools.text.TextComponent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class LangSetting extends AbstractLangSetting<LangSetting.Config> {
    @Getter(lazy = true)
    private static final LangSetting instance = new LangSetting();

    private LangSetting() {
    }

    @Override
    public String originFilePath() {
        return "module/" + ModuleMain.instance.getId() + "/lang.yml";
    }

    @Override
    public String filePath() {
        return this.originFilePath();
    }

    @Override
    public void reload() {
        super.reload();
        TextComponent prefix = GlobalLangSetting.getInstance().getConfig().prefix();

        this.config = new Config(
                new Config.Commands(
                        new Config.Commands.TpaHere(
                                super.component("commands.tpahere.usage", prefix),
                                super.component("commands.tpahere.description", prefix),
                                super.component("commands.tpahere.message", prefix),
                                super.raw("commands.tpahere.request_message"),
                                super.component("commands.tpahere.in_delay", prefix),
                                super.component("commands.tpahere.no_request", prefix),
                                super.component("commands.tpahere.send_self", prefix),
                                new Config.Commands.TpaHere.Accept(
                                        super.component("commands.tpahere.accept.message", prefix),
                                        super.component("commands.tpahere.accept.accepted", prefix)
                                ),
                                new Config.Commands.TpaHere.Reject(
                                        super.component("commands.tpahere.reject.message", prefix),
                                        super.component("commands.tpahere.reject.rejected", prefix)
                                ),
                                new Config.Commands.TpaHere.Delay(
                                        super.component("commands.tpahere.delay.message", prefix),
                                        super.component("commands.tpahere.delay.time_out", prefix)
                                )
                        )
                )
        );
    }

    /**
     * 发给被请求方的那条带"接受/拒绝"按钮的消息。
     *
     * <p>{player} 必须在解析前按原文填进去：按钮的 {@code <click:run_command:"/tpahere accept {player}">}
     * 里占位符属于命令字符串，解析后再也换不掉，玩家点上去就发出 {@code /tpahere accept {player}}。
     *
     * @param player 发起请求的玩家名(纯文本)
     * @return 带按钮的消息文本
     */
    public TextComponent requestMessage(@NotNull String player) {
        return super.filled(
                this.config.commands().tpahere().requestMessageTemplate(),
                GlobalLangSetting.getInstance().getConfig().prefix(),
                Map.of("player", player)
        );
    }

    public record Config(
            Commands commands
    ) {
        public record Commands(
                TpaHere tpahere
        ) {
            public record TpaHere(
                    TextComponent usage,
                    TextComponent description,
                    TextComponent message,
                    String requestMessageTemplate,
                    TextComponent inDelay,
                    TextComponent noRequest,
                    TextComponent sendSelf,
                    Accept accept,
                    Reject reject,
                    Delay delay
            ) {
                public record Accept(
                        TextComponent message,
                        TextComponent accepted
                ) {
                }

                public record Reject(
                        TextComponent message,
                        TextComponent rejected
                ) {
                }

                public record Delay(
                        TextComponent message,
                        TextComponent timeOut
                ) {
                }
            }
        }
    }
}
