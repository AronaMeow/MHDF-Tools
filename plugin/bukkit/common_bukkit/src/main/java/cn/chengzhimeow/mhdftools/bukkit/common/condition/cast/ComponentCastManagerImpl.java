package cn.chengzhimeow.mhdftools.bukkit.common.condition.cast;

import cn.chengzhimeow.cccondition.CCCondition;
import cn.chengzhimeow.cccondition.exception.CastException;
import cn.chengzhimeow.cccondition.manager.CastManager;
import cn.chengzhimeow.mhdftools.message.ColorUtil;
import cn.chengzhimeow.mhdftools.text.TextComponent;

public final class ComponentCastManagerImpl implements CastManager {
    public Object cast(CCCondition ccCondition, Object value, Class<?> type) throws CastException {
        if (!(value instanceof String s)) throw new CastException(value, type);
        // 同 action 那边：条件字段可能声明成 adventure 的 Component，按字段实际类型给值。
        TextComponent colored = ColorUtil.color(s);
        return type == null || type.isInstance(colored) ? colored : colored.asComponent();
    }
}
