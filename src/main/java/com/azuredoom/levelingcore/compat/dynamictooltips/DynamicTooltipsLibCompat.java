package com.azuredoom.levelingcore.compat.dynamictooltips;

import org.herolias.tooltips.api.DynamicTooltipsApiProvider;

import com.azuredoom.levelingcore.LevelingCore;

public class DynamicTooltipsLibCompat {

    private static boolean registered = false;

    private DynamicTooltipsLibCompat() {}

    public static void register() {
        if (registered)
            return;
        registered = true;

        var api = DynamicTooltipsApiProvider.get();
        if (api == null)
            return;

        for (var entry : LevelingCore.itemLevelMapping.entrySet()) {
            var itemId = entry.getKey();
            var requiredLevel = entry.getValue();
            api.addGlobalLine(itemId, "<color is=\"#b5a077\">Required Level: " + requiredLevel + " </color>");
            // api.addGlobalLine(itemId, CommandLang.REQUIRED_LEVEL.param("level", requiredLevel).toString());
        }
    }
}
