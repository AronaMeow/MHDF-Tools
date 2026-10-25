package cn.chengzhimeow.mhdftools.bukkit.module.core.config;

import cn.chengzhimeow.mhdftools.bukkit.module.core.ModuleMain;
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
                super.component("command_info_format", prefix),
                new Config.Commands(
                        new Config.Commands.MHDFTools(
                                super.component("commands.mhdftools.usage", prefix),
                                super.component("commands.mhdftools.description", prefix),
                                new Config.Commands.MHDFTools.SubCommands(
                                        new Config.Commands.MHDFTools.SubCommands.Help(
                                                super.component("commands.mhdftools.sub_commands.help.usage", prefix),
                                                super.component("commands.mhdftools.sub_commands.help.description", prefix),
                                                super.component("commands.mhdftools.sub_commands.help.message", prefix)
                                        ),
                                        new Config.Commands.MHDFTools.SubCommands.Feature(
                                                super.component("commands.mhdftools.sub_commands.feature.usage", prefix),
                                                super.component("commands.mhdftools.sub_commands.feature.description", prefix),
                                                super.raw("commands.mhdftools.sub_commands.feature.message")
                                        ),
                                        new Config.Commands.MHDFTools.SubCommands.Modules(
                                                super.component("commands.mhdftools.sub_commands.modules.usage", prefix),
                                                super.component("commands.mhdftools.sub_commands.modules.description", prefix),
                                                super.component("commands.mhdftools.sub_commands.modules.message", prefix)
                                        ),
                                        new Config.Commands.MHDFTools.SubCommands.Reload(
                                                super.component("commands.mhdftools.sub_commands.reload.usage", prefix),
                                                super.component("commands.mhdftools.sub_commands.reload.description", prefix),
                                                super.component("commands.mhdftools.sub_commands.reload.message", prefix)
                                        )
                                )
                        )
                )
        );
    }

    /**
     * <code>/{command} feature</code> 的那条带翻页按钮的帮助消息。
     *
     * <p>{command}、{last_page}、{next_page} 写在 {@code <click:run_command:"/{command} feature {last_page}">}
     * 的命令字符串里，解析之后就换不掉了(点了会发出带着字面量占位符的命令)，
     * 所以这几个要在解析前按原文填好。{help_list}、{page}、{max_page} 只在正文里出现，
     * 由调用方继续做组件级替换。
     *
     * @param command  主命令名(纯文本)
     * @param lastPage 上一页页码
     * @param nextPage 下一页页码
     * @return 帮助消息文本
     */
    public TextComponent featureMessage(@NotNull String command, int lastPage, int nextPage) {
        return super.filled(
                this.config.commands().mhdftools().subCommands().feature().messageTemplate(),
                GlobalLangSetting.getInstance().getConfig().prefix(),
                Map.of(
                        "command", command,
                        "last_page", String.valueOf(lastPage),
                        "next_page", String.valueOf(nextPage)
                )
        );
    }

    public record Config(
            TextComponent commandInfoFormat,
            Commands commands
    ) {
        public record Commands(
                MHDFTools mhdftools
        ) {
            public record MHDFTools(
                    TextComponent usage,
                    TextComponent description,
                    SubCommands subCommands
            ) {
                public record SubCommands(
                        Help help,
                        Feature feature,
                        Modules modules,
                        Reload reload
                ) {
                    public record Help(
                            TextComponent usage,
                            TextComponent description,
                            TextComponent message
                    ) {
                    }

                    public record Feature(
                            TextComponent usage,
                            TextComponent description,
                            String messageTemplate
                    ) {
                    }

                    public record Modules(
                            TextComponent usage,
                            TextComponent description,
                            TextComponent message
                    ) {
                    }

                    public record Reload(
                            TextComponent usage,
                            TextComponent description,
                            TextComponent message
                    ) {
                    }
                }
            }
        }
    }
}
