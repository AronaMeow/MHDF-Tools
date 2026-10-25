package cn.chengzhimeow.mhdftools.text;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;


/**
 * MHDF TextComponent wrapper for Adventure 5.
 *
 * This class intentionally does NOT implement Adventure TextComponent.
 * Adventure 5 no longer allows third-party implementations of Component types.
 */
public final class TextComponent implements ComponentLike {

    private final Component source;

    public TextComponent() {
        this(Component.empty());
    }

    public TextComponent(String content) {
        this(Component.text(content));
    }

    public TextComponent(Component component) {
        this.source = Objects.requireNonNull(component);
    }

    public TextComponent(TextComponent component) {
        this.source = component == null
                ? Component.empty()
                : component.asComponent();
    }

    public Component component() {
        return source;
    }

    @Override
    public Component asComponent() {
        return source;
    }

    public TextComponent copy() {
        return new TextComponent(source);
    }

    public TextComponent replace(String target, String replacement) {
        return replaceText(target, replacement == null ? Component.empty() : Component.text(replacement), false);
    }

    public TextComponent replace(String target, Component replacement) {
        return replaceText(target, replacement == null ? Component.empty() : replacement, false);
    }

    public TextComponent replace(String target, TextComponent replacement) {
        return replace(target, replacement == null ? Component.empty() : replacement.asComponent());
    }

    public TextComponent replaceFirst(String target, String replacement) {
        return replaceText(target, replacement == null ? Component.empty() : Component.text(replacement), true);
    }

    public TextComponent replaceFirst(String target, Component replacement) {
        return replaceText(target, replacement == null ? Component.empty() : replacement, true);
    }

    public TextComponent replaceFirst(String target, TextComponent replacement) {
        return replaceFirst(target, replacement == null ? Component.empty() : replacement.asComponent());
    }

    /**
     * 先交给 Adventure 的 replaceText，只有在它一次都匹配不到的时候才走跨兄弟节点兜底。
     * MiniMessage 的 &lt;gradient&gt;/&lt;rainbow&gt; 这类标签会把作用域里的文本逐字拆成独立组件，
     * replaceText 只在单个组件自己的 content 里查找，所以像
     * {@code <gradient:#86D7F7:#EAB5B5>{player}</gradient>} 这样的格式匹配不到 {player}。
     * 加这道闸门是为了不重复替换：玩家自己发出来的 {message} 字面量在被插入之后不该再被扫到一遍。
     * 代价是同一条文本里既有普通叶子又有拆分叶子时，只处理普通叶子那种。
     *
     * <p>注意：replaceText 只处理文本节点，{@code <click:run_command:"...">} 里的命令值它碰不到，
     * 而且命令值是字符串、也塞不进组件。带点击命令的模板要在解析前用原文填充占位符，
     * 见 {@link cn.chengzhimeow.mhdftools.config.AbstractLangSetting#filled}。
     */
    private TextComponent replaceText(String target, Component replacement, boolean firstOnly) {
        if (target == null || target.isEmpty()) return new TextComponent(source);
        TextReplacementConfig.Builder builder = TextReplacementConfig.builder()
                .matchLiteral(target)
                .replacement(replacement);
        if (firstOnly) builder.times(1);
        Component replaced = source.replaceText(builder.build());
        if (containsText(source, target)) return new TextComponent(replaced);
        return new TextComponent(replaceAcrossSiblings(replaced, target, replacement, new int[]{firstOnly ? 1 : Integer.MAX_VALUE}));
    }

    /**
     * 是否有单个文本组件已经包含目标串，即 Adventure 能处理的情形。
     */
    private static boolean containsText(Component node, String target) {
        if (target == null || target.isEmpty()) return false;
        if (node instanceof net.kyori.adventure.text.TextComponent text && text.content().contains(target)) return true;
        for (Component child : node.children()) {
            if (containsText(child, target)) return true;
        }
        return false;
    }

    /**
     * 跨兄弟节点兜底替换：把同一父节点下连续的纯文本兄弟拼成一条文本流，在流上找目标串。
     * 命中的文本按原样式保留下来，替换组件挂在匹配起点所在兄弟的样式下。
     * 已知边界：匹配跨过非文本组件（如 translatable）或跨不同父节点时不生效。
     */
    private static Component replaceAcrossSiblings(Component node, String target, Component replacement, int[] budget) {
        if (target == null || target.isEmpty() || replacement == null) return node;
        List<Component> children = node.children();
        if (children.isEmpty()) return node;

        List<Component> rewritten = new ArrayList<>(children.size());
        for (Component child : children) {
            rewritten.add(replaceAcrossSiblings(child, target, replacement, budget));
        }
        List<Component> merged = mergeSiblings(rewritten, target, replacement, budget);
        return merged == rewritten ? node : node.children(merged);
    }

    /**
     * 在连续的文本兄弟之间做一次跨节点匹配，返回替换后的兄弟列表。
     * 没有跨节点匹配时原样返回入参列表（同一个引用），方便调用方判断有没有改动。
     */
    private static List<Component> mergeSiblings(List<Component> children, String target, Component replacement, int[] budget) {
        List<Component> result = null;
        int index = 0;
        while (index < children.size()) {
            if (spansable(children.get(index))) {
                int start = index;
                StringBuilder flow = new StringBuilder();
                while (index < children.size() && spansable(children.get(index))) {
                    flow.append(((net.kyori.adventure.text.TextComponent) children.get(index)).content());
                    index++;
                }
                List<Component> run = mergeRun(children.subList(start, index), flow.toString(), target, replacement, budget);
                if (result == null) result = new ArrayList<>(children.size());
                result.addAll(run);
            } else {
                if (result == null) result = new ArrayList<>(children.size());
                result.add(children.get(index));
                index++;
            }
        }
        return result == null ? children : result;
    }

    /**
     * 处理一段连续文本兄弟：把拼出来的文本流里所有目标串替换掉，按字符规划后逐兄弟重建。
     * 匹配完整落在单个兄弟里的情况交给 Adventure 处理，这里只处理它匹配不到的跨节点匹配。
     */
    private static List<Component> mergeRun(List<Component> run, String flow, String target, Component replacement, int[] budget) {
        List<int[]> matches = new ArrayList<>();
        int from = 0;
        while (budget[0] > 0) {
            int at = flow.indexOf(target, from);
            if (at < 0) break;
            matches.add(new int[]{at, at + target.length()});
            budget[0]--;
            from = at + target.length();
        }
        if (matches.isEmpty()) return run;

        // 规划表：consumed = 该字符被某个匹配吃掉，insert = 匹配起点(在此处放替换组件)
        boolean[] consumed = new boolean[flow.length()];
        boolean[] insert = new boolean[flow.length()];
        for (int[] match : matches) {
            insert[match[0]] = true;
            for (int p = match[0]; p < match[1]; p++) consumed[p] = true;
        }

        List<Component> result = new ArrayList<>(run.size() + matches.size());
        int offset = 0;
        for (Component leaf : run) {
            String content = ((net.kyori.adventure.text.TextComponent) leaf).content();
            StringBuilder keep = new StringBuilder(content.length());
            for (int p = offset; p < offset + content.length(); p++) {
                if (insert[p]) {
                    if (!keep.isEmpty()) {
                        result.add(piece(leaf, keep.toString()));
                        keep.setLength(0);
                    }
                    result.add(piece(leaf, "").append(replacement));
                }
                if (!consumed[p]) keep.append(flow.charAt(p));
            }
            if (!keep.isEmpty()) result.add(piece(leaf, keep.toString()));
            offset += content.length();
        }
        return result;
    }

    private static boolean spansable(Component component) {
        return component instanceof net.kyori.adventure.text.TextComponent text && text.children().isEmpty();
    }

    /**
     * 保留兄弟节点原有样式，只换掉文本内容。
     */
    private static Component piece(Component leaf, String content) {
        return ((net.kyori.adventure.text.TextComponent) leaf).toBuilder().content(content).build();
    }

    public TextComponent append(Component component) {
        return new TextComponent(source.append(component));
    }

    public TextComponent append(TextComponent component) {
        return append(component == null ? Component.empty() : component.asComponent());
    }

    public TextComponent color(TextColor color) {
        return new TextComponent(source.color(color));
    }

    public TextComponent clickEvent(ClickEvent event) {
        return new TextComponent(source.clickEvent(event));
    }

    public TextComponent hoverEvent(HoverEvent<?> event) {
        return new TextComponent(source.hoverEvent(event));
    }

    public TextComponent transform(UnaryOperator<Component> operator) {
        return new TextComponent(operator.apply(source));
    }

    public String toMiniMessageString() {
        return MiniMessage.miniMessage().serialize(source);
    }

    public JsonElement toJsonElement() {
        return GsonComponentSerializer.gson().serializeToTree(source);
    }

    public String toJsonString() {
        return JSONComponentSerializer.json().serialize(source);
    }

    public String toLegacyString() {
        return LegacyComponentSerializer.legacySection().serialize(source);
    }

    public String content() {
        if (source instanceof net.kyori.adventure.text.TextComponent text) {
            return text.content();
        }
        return "";
    }

    @Override
    public String toString() {
        return toMiniMessageString();
    }
}
