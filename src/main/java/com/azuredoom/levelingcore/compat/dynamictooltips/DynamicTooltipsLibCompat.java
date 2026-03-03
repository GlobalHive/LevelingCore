package com.azuredoom.levelingcore.compat.dynamictooltips;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.herolias.tooltips.api.DynamicTooltipsApiProvider;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azuredoom.levelingcore.LevelingCore;

public class DynamicTooltipsLibCompat {

    private static boolean registered = false;

    private static final Pattern XRAY_PATTERN = Pattern.compile(
        "(?:\"|'|\\b)(Physical|Magical|Void|True|Poison|Fire|Ice|Wind|Earth|Water|Lightning|Elemental)(?:\"|'|\\b)\\s*[:=]\\s*(\\d+)",
        2
    );

    private final Set<String> processedItems = ConcurrentHashMap.newKeySet();

    public static final DynamicTooltipsLibCompat INSTANCE = new DynamicTooltipsLibCompat();

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
        }

        new Timer().schedule(new TimerTask() {

            {
                Objects.requireNonNull(DynamicTooltipsLibCompat.INSTANCE);
            }

            public void run() {
                try {
                    int itemCount = Item.getAssetMap() != null ? Item.getAssetMap().getAssetMap().size() : 0;
                    if (itemCount < 100) {
                        return;
                    }

                    DynamicTooltipsLibCompat.INSTANCE.scanWeapons();
                } catch (Exception var2) {}
            }
        }, 1000L, 1000L);
    }

    private void scanWeapons() {
        try {
            var allItems = Item.getAssetMap().getAssetMap().values();

            for (Item item : allItems) {
                if (!this.processedItems.contains(item.getId()) && this.hasWeaponTag(item)) {
                    List<Integer> damages = this.getDamagesFromBuffer(item);
                    if (damages.isEmpty()) {
                        this.processedItems.add(item.getId());
                    } else if (this.processItem(item, damages)) {
                        this.processedItems.add(item.getId());
                    }
                }
            }
        } catch (Exception e) {}
    }

    private boolean processItem(Item item, List<Integer> damages) {
        var api = DynamicTooltipsApiProvider.get();
        if (api == null)
            return false;

        try {
            if (!damages.isEmpty()) {
                StringBuilder text = new StringBuilder();

                int min = (Integer) damages.get(0);
                int max = (Integer) damages.get(damages.size() - 1);
                text.append(String.format("<color is=\"#b5a077\">Weapon Level: %d</color>\n", item.getItemLevel()));
                text.append(String.format("<color is=\"#b5a077\">Weapon Damage: %d - %d</color>", min, max));
                api.addGlobalLine(item.getId(), text.toString());
            }
            return true;
        } catch (Exception var12) {
            return false;
        }
    }

    private List<Integer> getDamagesFromBuffer(Item item) {
        List<Integer> damages = new ArrayList();
        StringBuilder hugeDump = new StringBuilder();

        try {
            this.crawlAndExtractText(item, hugeDump, 0);
            Matcher m = XRAY_PATTERN.matcher(hugeDump.toString());

            while (m.find()) {
                try {
                    int val = Integer.parseInt(m.group(2));
                    if (val > 0) {
                        damages.add(val);
                    }
                } catch (Exception var6) {}
            }
        } catch (Exception var7) {}

        Collections.sort(damages);
        return damages;
    }

    private void crawlAndExtractText(Object obj, StringBuilder sb, int depth) {
        if (obj != null && depth <= 8) {
            try {
                Field bufField = this.getField(obj.getClass(), "buffer");
                if (bufField != null) {
                    Object buf = bufField.get(obj);
                    if (buf != null) {
                        if (buf instanceof byte[]) {
                            sb.append(new String((byte[]) buf, StandardCharsets.UTF_8)).append(" ");
                        } else if (buf instanceof char[]) {
                            sb.append(new String((char[]) buf)).append(" ");
                        }
                    }
                }

                if (obj instanceof Map) {
                    for (Object val : ((Map) obj).values()) {
                        this.crawlAndExtractText(val, sb, depth + 1);
                    }
                } else if (obj instanceof Iterable) {
                    for (Object val : (Iterable) obj) {
                        this.crawlAndExtractText(val, sb, depth + 1);
                    }
                } else if (obj.getClass().isArray()) {
                    int len = Array.getLength(obj);

                    for (int i = 0; i < len; ++i) {
                        this.crawlAndExtractText(Array.get(obj, i), sb, depth + 1);
                    }
                } else if (this.isComplex(obj.getClass())) {
                    for (Field f : obj.getClass().getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers())) {
                            f.setAccessible(true);
                            this.crawlAndExtractText(f.get(obj), sb, depth + 1);
                        }
                    }
                }
            } catch (Exception var9) {}

        }
    }

    private boolean isComplex(Class<?> c) {
        return !c.isPrimitive() && !c.getName().startsWith("java.") && !c.isEnum();
    }

    private boolean hasWeaponTag(Item item) {
        if (item.getWeapon() != null) {
            return true;
        } else {
            String id = item.getId().toLowerCase();
            return id.contains("sword") || id.contains("axe") || id.contains("staff") || id.contains("bow") || id
                .contains("hammer") || id.contains("dagger");
        }
    }

    private Field getField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException var4) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

}
