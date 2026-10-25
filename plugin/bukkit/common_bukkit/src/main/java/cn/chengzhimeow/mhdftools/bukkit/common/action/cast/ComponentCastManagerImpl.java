package cn.chengzhimeow.mhdftools.bukkit.common.action.cast;

import cn.chengzhimeow.ccaction.CCAction;
import cn.chengzhimeow.ccaction.exception.CastException;
import cn.chengzhimeow.ccaction.manager.CastManager;
import cn.chengzhimeow.mhdftools.message.ColorUtil;
import cn.chengzhimeow.mhdftools.text.TextComponent;

public final class ComponentCastManagerImpl implements CastManager {
    public Object cast(CCAction ccAction, Object value, Class<?> type) throws CastException {
        if (!(value instanceof String s)) throw new CastException(value, type);
        // ColorUtil 返回的是包装类，而动作字段可能声明成 adventure 的 Component，
        // 必须按字段实际类型给值，否则 AbstractAction.init 里的 Field.set 会抛 IllegalArgumentException。
        TextComponent colored = ColorUtil.color(s);
        return type == null || type.isInstance(colored) ? colored : colored.asComponent();
    }
}
