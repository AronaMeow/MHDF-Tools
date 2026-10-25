package cn.chengzhimeow.mhdftools.message;

import cn.chengzhimeow.mhdftools.text.TextComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ColorUtil {

    /**
     * 检测字符是否是颜色代码的字符
     *
     * @param c 字符
     * @return 结果
     */
    private static boolean notColorCode(char c) {
        return c != '§' && c != '&';
    }

    /**
     * 检测字符是否是十六进制字符
     *
     * @param c 字符
     * @return 结果
     */
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    /**
     * 检测从 from 开始的 count 个字符是否全部是十六进制字符
     *
     * @param chars 字符数组
     * @param from  起始下标(包含)
     * @param count 长度
     * @return 结果
     */
    private static boolean hasHexDigits(@NotNull char[] chars, int from, int count) {
        if (from < 0 || from + count > chars.length) return false;
        for (int i = from; i < from + count; i++) {
            if (!ColorUtil.isHexDigit(chars[i])) return false;
        }
        return true;
    }

    /**
     * 检测 index 处的字符不会把这段十六进制变成 miniMessage 标签的参数
     * (&lt;gradient:#F18989:#EAB5B5&gt; 中的 #F18989 后面是 ':'，#EAB5B5 后面是 '>'，
     * 它们本身就是标签的一部分，不能再包一层 &lt;#...&gt;)
     *
     * @param chars 字符数组
     * @param index 待检测的下标
     * @return 结果
     */
    private static boolean notTagArgumentEnd(@NotNull char[] chars, int index) {
        return index >= chars.length || (chars[index] != ':' && chars[index] != '>');
    }

    /**
     * 将旧版颜色字符文本转换为miniMessage格式
     *
     * @param legacy 旧版颜色字符文本
     * @return miniMessage格式文本
     */
    private static String legacyToMiniMessage(@NotNull String legacy) {
        StringBuilder stringBuilder = new StringBuilder("<!i>");
        char[] chars = legacy.toCharArray();

        loop:
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            // 裸 #RRGGBB 直接补全成 miniMessage 颜色标签
            // 紧跟在 '<' 之后的(整个已经是标签, 如 <#aabbcc>)以及后面跟着 ':' 或 '>' 的(是标签参数,
            // 如 <gradient:#F18989:#EAB5B5>、</#F18989>)都不能包装, 否则会把整个标签解析坏
            if (c == '#'
                    && (i == 0 || chars[i - 1] != '<')
                    && ColorUtil.hasHexDigits(chars, i + 1, 6)
                    && ColorUtil.notTagArgumentEnd(chars, i + 7)) {
                stringBuilder.append("<#").append(chars, i + 1, 6).append(">");
                i += 6;
                continue;
            }

            if (ColorUtil.notColorCode(c)) {
                stringBuilder.append(c);
                continue;
            }

            if (i + 1 >= chars.length) {
                stringBuilder.append(c);
                continue;
            }

            switch (chars[i + 1]) {
                case '0' -> stringBuilder.append("<black>");
                case '1' -> stringBuilder.append("<dark_blue>");
                case '2' -> stringBuilder.append("<dark_green>");
                case '3' -> stringBuilder.append("<dark_aqua>");
                case '4' -> stringBuilder.append("<dark_red>");
                case '5' -> stringBuilder.append("<dark_purple>");
                case '6' -> stringBuilder.append("<gold>");
                case '7' -> stringBuilder.append("<gray>");
                case '8' -> stringBuilder.append("<dark_gray>");
                case '9' -> stringBuilder.append("<blue>");
                case 'a' -> stringBuilder.append("<green>");
                case 'b' -> stringBuilder.append("<aqua>");
                case 'c' -> stringBuilder.append("<red>");
                case 'd' -> stringBuilder.append("<light_purple>");
                case 'e' -> stringBuilder.append("<yellow>");
                case 'f' -> stringBuilder.append("<white>");
                case 'r' -> stringBuilder.append("<reset>");
                case 'l' -> stringBuilder.append("<b>");
                case 'm' -> stringBuilder.append("<st>");
                case 'o' -> stringBuilder.append("<i>");
                case 'n' -> stringBuilder.append("<u>");
                case 'k' -> stringBuilder.append("<obf>");
                case '#' -> {
                    // &#RRGGBB / §#RRGGBB 是显式的十六进制颜色写法, 即使在标签内部也要转换
                    if (ColorUtil.hasHexDigits(chars, i + 2, 6)) {
                        stringBuilder.append("<#").append(chars, i + 2, 6).append(">");
                        i += 6;
                    } else {
                        stringBuilder.append(c);
                        continue loop;
                    }
                }
                case 'x' -> {
                    if (i + 13 >= chars.length
                            || ColorUtil.notColorCode(chars[i + 2])
                            || ColorUtil.notColorCode(chars[i + 4])
                            || ColorUtil.notColorCode(chars[i + 6])
                            || ColorUtil.notColorCode(chars[i + 8])
                            || ColorUtil.notColorCode(chars[i + 10])
                            || ColorUtil.notColorCode(chars[i + 12])
                    ) {
                        stringBuilder.append(c);
                        continue loop;
                    }
                    stringBuilder
                            .append("<#")
                            .append(chars[i + 3])
                            .append(chars[i + 5])
                            .append(chars[i + 7])
                            .append(chars[i + 9])
                            .append(chars[i + 11])
                            .append(chars[i + 13])
                            .append(">");
                    i += 12;
                }
                default -> {
                    // 不是已知的颜色代码, 原样输出, 后面的字符照常处理
                    stringBuilder.append(c);
                    continue loop;
                }
            }
            i++;
        }
        return stringBuilder.toString();
    }

    /**
     * 处理miniMessage颜色符号
     *
     * @param message 文本
     * @return 处理后的文本
     */
    public static TextComponent color(String message) {
        if (message == null) return new TextComponent();
        return MiniMessageUtil.miniMessage(ColorUtil.legacyToMiniMessage(message));
    }

    /**
     * 处理miniMessage颜色符号, 并把 {key} 占位符替换成给定的文本实例
     *
     * <p>占位符是在解析成文本组件之前以标签的形式插入的, 所以写在
     * &lt;gradient:#AABBCC:#DDEEFF&gt;、&lt;rainbow&gt; 这类会把文本按字符逐字拆开的标签里也能正常替换。
     * 组件级的字符串替换(如 {@link TextComponent#replace(String, TextComponent)})在这种标签里匹配不到
     * 占位符, 会让 {@code {message}} 这类占位符原样显示出来。
     *
     * @param message      文本
     * @param placeholders 占位符(key 不带花括号, value 是要插入的文本实例)
     * @return 处理后的文本
     */
    public static TextComponent color(@Nullable String message, @Nullable Map<String, ? extends ComponentLike> placeholders) {
        if (message == null) return new TextComponent();
        if (placeholders == null || placeholders.isEmpty()) return ColorUtil.color(message);

        String miniMessage = ColorUtil.legacyToMiniMessage(message);

        // 排序保证同一份参数生成的内部标签名稳定
        List<String> keys = new ArrayList<>(placeholders.keySet());
        Collections.sort(keys);

        List<TagResolver> resolverList = new ArrayList<>(keys.size());
        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            String token = "{" + key + "}";
            if (!miniMessage.contains(token)) continue;

            ComponentLike value = placeholders.get(key);
            // 标签名只保留字母数字和下划线/中划线, 带上下标防止不同的 key 相互覆盖
            String tag = "mhdft-" + index + "-" + key.replaceAll("[^0-9A-Za-z_-]", "");

            miniMessage = miniMessage.replace(token, "<" + tag + ">");
            resolverList.add(TagResolver.resolver(tag, Tag.selfClosingInserting(
                    value == null ? Component.empty() : value.asComponent())));
        }

        return MiniMessageUtil.miniMessage(miniMessage, resolverList.toArray(new TagResolver[0]));
    }
}
