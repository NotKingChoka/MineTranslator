package net.kingchoka.minetranslatorport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ReflectionAccess {
    private static Object translateKeyMapping;
    private static Object configKeyMapping;
    private static boolean keyMappingsRegistered;

    private ReflectionAccess() {}

    static synchronized void ensureKeyMappings(Object client, File gameDirectory) {
        if (keyMappingsRegistered || client == null) return;
        try {
            Object options = findFieldValue(client, "net.minecraft.class_315");
            if (options == null) options = findFieldValue(client, "net.minecraft.client.Options");
            if (options == null) return;

            Class<?> keyType;
            try {
                keyType = Class.forName("net.minecraft.class_304");
            } catch (ClassNotFoundException ignored) {
                keyType = Class.forName("net.minecraft.client.KeyMapping");
            }

            Constructor<?> constructor = null;
            for (Constructor<?> candidate : keyType.getDeclaredConstructors()) {
                Class<?>[] parameters = candidate.getParameterTypes();
                if (parameters.length == 3 && parameters[0] == String.class
                    && parameters[1] == int.class && parameters[2] == String.class) {
                    constructor = candidate;
                    break;
                }
            }
            if (constructor == null) return;
            constructor.setAccessible(true);

            registerKeyCategory(keyType, "key.categories.minetranslator");
            translateKeyMapping = constructor.newInstance(
                "key.MineTranslator.translate", 96, "key.categories.minetranslator");
            configKeyMapping = constructor.newInstance(
                "key.MineTranslator.config", 79, "key.categories.minetranslator");

            Field mappingsField = null;
            int largestLength = -1;
            for (Field field : allFields(options.getClass())) {
                if (!field.getType().isArray() || field.getType().getComponentType() != keyType) continue;
                field.setAccessible(true);
                Object value = field.get(options);
                int length = value == null ? 0 : Array.getLength(value);
                if (length > largestLength) {
                    mappingsField = field;
                    largestLength = length;
                }
            }
            if (mappingsField == null) return;

            Object current = mappingsField.get(options);
            Object appended = Array.newInstance(keyType, largestLength + 2);
            if (current != null) System.arraycopy(current, 0, appended, 0, largestLength);
            Array.set(appended, largestLength, translateKeyMapping);
            Array.set(appended, largestLength + 1, configKeyMapping);
            mappingsField.set(options, appended);

            loadSavedKeyMappings(keyType, gameDirectory);
            callStatic(keyType, "method_1424");
            callStatic(keyType, "resetMapping");
            keyMappingsRegistered = true;
            System.out.println("[MineTranslator] Key mappings registered: translate=Ё, config=O/Щ");
        } catch (Exception exception) {
            System.out.println("[MineTranslator] Failed to register key mappings: " + exception.getClass().getSimpleName());
        }
    }

    static int translateKeyCode() { return keyCode(translateKeyMapping, 96); }
    static int configKeyCode() { return keyCode(configKeyMapping, 79); }

    public static void openControls(Object client) {
        if (client == null) return;
        Object parent = currentScreen(client);
        
        // Find options field value
        Object options = findFieldValue(client, "field_1690"); // Intermediary for options
        if (options == null) options = findFieldValue(client, "options"); // Mojmap/Yarn
        if (options == null) options = findFieldValue(client, "net.minecraft.class_315");
        if (options == null) options = findFieldValue(client, "net.minecraft.client.Options");
        
        if (options == null) {
            System.out.println("[MineTranslator] Failed to find options field in client");
            return;
        }

        // Try classes for the controls screen
        String[] controlsClasses = {
            "net.minecraft.class_6599", // KeybindsScreen (1.19.4+ Intermediary)
            "net.minecraft.class_458",  // ControlsOptionsScreen / ControlsScreen (1.16.5-1.19.2 Intermediary)
            "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen", // Mojmap 1.20+
            "net.minecraft.client.gui.screen.option.KeybindsScreen",            // Yarn 1.20+
            "net.minecraft.client.gui.screens.controls.KeyBindsScreen",
            "net.minecraft.client.gui.screens.controls.ControlsScreen",
            "net.minecraft.client.gui.screens.options.controls.ControlsScreen",
            "net.minecraft.client.gui.screen.options.ControlsOptionsScreen"
        };

        Class<?> screenType = null;
        try { screenType = Class.forName("net.minecraft.class_437"); }
        catch (ClassNotFoundException e) {
            try { screenType = Class.forName("net.minecraft.client.gui.screens.Screen"); }
            catch (ClassNotFoundException ignored) {}
        }
        Class<?> optionsType = options.getClass();
        System.out.println("[MineTranslator Debug] openControls: options class is " + optionsType.getName() + ", screenType is " + (screenType == null ? "null" : screenType.getName()));

        for (String className : controlsClasses) {
            try {
                System.out.println("[MineTranslator Debug] Trying to load controls class: " + className);
                Class<?> controlsType = Class.forName(className);
                System.out.println("[MineTranslator Debug] Loaded controls class: " + className);
                Constructor<?> constructor = null;
                
                // Find constructor taking (Screen, Options)
                for (Constructor<?> c : controlsType.getDeclaredConstructors()) {
                    Class<?>[] params = c.getParameterTypes();
                    System.out.println("[MineTranslator Debug]   Constructor has " + params.length + " parameters");
                    if (params.length == 2) {
                        System.out.println("[MineTranslator Debug]     param0: " + params[0].getName() + " (assignable from Screen: " + (screenType != null && params[0].isAssignableFrom(screenType)) + ", assigns Screen: " + (screenType != null && screenType.isAssignableFrom(params[0])) + ")");
                        System.out.println("[MineTranslator Debug]     param1: " + params[1].getName() + " (assignable from Options: " + params[1].isAssignableFrom(optionsType) + ")");
                        if (screenType != null && screenType.isAssignableFrom(params[0]) && 
                            params[1].isAssignableFrom(optionsType)) {
                            constructor = c;
                            break;
                        }
                    }
                }
                
                if (constructor != null) {
                    constructor.setAccessible(true);
                    Object controls = constructor.newInstance(parent, options);
                    
                    // setScreen method invocation
                    try {
                        invokeStrict(client, "method_1507", controls); // Intermediary
                    } catch (NoSuchMethodException e) {
                        invokeStrict(client, "setScreen", controls); // Mojmap/Yarn
                    }
                    System.out.println("[MineTranslator] Opened controls screen: " + className);
                    return;
                } else {
                    System.out.println("[MineTranslator Debug] Suitable constructor not found for class: " + className);
                }
            } catch (ClassNotFoundException e) {
                System.out.println("[MineTranslator Debug] Class not found: " + className);
            } catch (Exception e) {
                System.out.println("[MineTranslator] Failed to instantiate controls screen " + className + ": " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        
        restoreScreen(client, parent);
        System.out.println("[MineTranslator] Failed to open any controls screen: ClassNotFoundException");
    }

    @SuppressWarnings("unchecked")
    private static void registerKeyCategory(Class<?> keyType, String category) {
        try {
            Field orderField = findField(keyType, "field_1656");
            if (orderField == null || !Modifier.isStatic(orderField.getModifiers())) return;
            orderField.setAccessible(true);
            Object value = orderField.get(null);
            if (!(value instanceof Map)) return;
            Map<Object, Object> order = (Map<Object, Object>) value;
            if (order.containsKey(category)) return;
            int next = 1;
            for (Object index : order.values()) {
                if (index instanceof Number) next = Math.max(next, ((Number) index).intValue() + 1);
            }
            order.put(category, next);
        } catch (Exception ignored) {}
    }

    private static void restoreScreen(Object client, Object parent) {
        try {
            if (hasClassInHierarchy(client, "net.minecraft.class_310")) {
                invokeStrict(client, "method_1507", parent);
            } else {
                invokeStrict(client, "setScreen", parent);
            }
        } catch (Exception ignored) {}
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }

    private static void loadSavedKeyMappings(Class<?> keyType, File gameDirectory) {
        if (gameDirectory == null) return;
        File options = new File(gameDirectory, "options.txt");
        if (!options.isFile()) return;
        try {
            List<String> lines = Files.readAllLines(options.toPath(), Charset.forName("UTF-8"));
            for (String line : lines) {
                if (line.startsWith("key_key.MineTranslator.translate:")) {
                    setSavedKey(translateKeyMapping, line.substring(line.indexOf(':') + 1));
                } else if (line.startsWith("key_key.MineTranslator.config:")) {
                    setSavedKey(configKeyMapping, line.substring(line.indexOf(':') + 1));
                }
            }
        } catch (Exception ignored) {}
    }

    private static void setSavedKey(Object mapping, String serialized) {
        if (mapping == null || serialized == null || serialized.isEmpty()) return;
        try {
            Class<?> input = Class.forName("net.minecraft.class_3675");
            Object key = callStatic(input, "method_15981", serialized);
            if (key != null) call(mapping, "method_1422", key);
        } catch (Exception ignored) {}
    }

    private static int keyCode(Object mapping, int fallback) {
        if (mapping == null) return fallback;
        try {
            Field currentKey = findField(mapping.getClass(), "field_1655");
            if (currentKey != null) {
                currentKey.setAccessible(true);
                Object key = currentKey.get(mapping);
                Object value = call(key, "method_1444");
                if (value instanceof Number) return ((Number) value).intValue();
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    static Object minecraft() {
        String[] names = {"net.minecraft.class_310", "net.minecraft.client.Minecraft"};
        for (String name : names) try {
            Class<?> type = Class.forName(name);
            Object direct = callStatic(type, "method_1551");
            if (direct == null) direct = callStatic(type, "getInstance");
            if (direct != null) return direct;
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) return value;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    static Object currentScreen(Object client) {
        Object screen = findFieldValue(client, "net.minecraft.class_437");
        return screen != null ? screen : findFieldValue(client, "net.minecraft.client.gui.screens.Screen");
    }

    static Object textField(Object screen) {
        Object field = findFieldValue(screen, "net.minecraft.class_342");
        return field != null ? field : findFieldValue(screen, "net.minecraft.client.gui.components.EditBox");
    }

    static String inputText(Object textField) {
        Object value = call(textField, "method_1882");
        if (!(value instanceof String)) value = call(textField, "getValue");
        return value instanceof String ? (String) value : "";
    }

    static void setInputText(Object textField, String text) {
        Object result = call(textField, "method_1852", text);
        if (result == null) call(textField, "setValue", text);
    }

    static boolean isChatScreen(Object screen) {
        return hasClassInHierarchy(screen, "net.minecraft.class_408")
            || hasClassInHierarchy(screen, "net.minecraft.client.gui.screens.ChatScreen");
    }

    static String text(Object component) {
        if (component == null) return "";
        // In 1.16.5 method_10851() is empty for translatable components such as
        // vanilla item names. The public StringVisitable#getString() resolves the
        // language key and returns exactly what the player sees (for example Anvil).
        Object value = callPublic(component, "getString");
        if (!(value instanceof String) || ((String) value).isEmpty()) value = call(component, "getString");
        if (!(value instanceof String) || ((String) value).isEmpty()) value = call(component, "method_10851");
        if (value instanceof String && !((String) value).isEmpty()) return (String) value;
        String serialized = visibleTextFromLegacyJson(component);
        if (serialized != null && !serialized.isEmpty()) return serialized;
        String resolved = resolveLegacyTranslatable(component);
        // Never return component.toString(): it contains Style{clickEvent=...,
        // hoverEvent=..., font=...} and must not be sent to a translator.
        return resolved != null ? resolved : "";
    }

    private static String visibleTextFromLegacyJson(Object component) {
        try {
            Class<?> serializer = Class.forName("net.minecraft.class_2561$class_2562");
            Object json = callStatic(serializer, "method_10867", component);
            if (!(json instanceof String) || ((String) json).isEmpty()) return null;
            return visibleTextFromJson(new JsonParser().parse((String) json));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String visibleTextFromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonElement child : element.getAsJsonArray()) result.append(visibleTextFromJson(child));
            return result.toString();
        }
        if (!element.isJsonObject()) return "";
        JsonObject object = element.getAsJsonObject();
        StringBuilder result = new StringBuilder();
        if (object.has("text")) {
            result.append(visibleTextFromJson(object.get("text")));
        } else if (object.has("translate")) {
            String key = object.get("translate").getAsString();
            String translated = resolveLegacyTranslationKey(key);
            result.append(translated == null ? humanizeTranslationKey(key) : translated);
        } else if (object.has("keybind")) {
            result.append(humanizeTranslationKey(object.get("keybind").getAsString()));
        } else if (object.has("selector")) {
            result.append(object.get("selector").getAsString());
        } else if (object.has("score") && object.get("score").isJsonObject()) {
            JsonObject score = object.getAsJsonObject("score");
            if (score.has("value")) result.append(score.get("value").getAsString());
            else if (score.has("name")) result.append(score.get("name").getAsString());
        }
        if (object.has("extra") && object.get("extra").isJsonArray()) {
            JsonArray extra = object.getAsJsonArray("extra");
            for (JsonElement child : extra) result.append(visibleTextFromJson(child));
        }
        return result.toString();
    }

    private static String resolveLegacyTranslatable(Object component) {
        Object keyValue = call(component, "method_11022");
        if (!(keyValue instanceof String) || ((String) keyValue).isEmpty()) return null;
        String key = (String) keyValue;
        Object arguments = call(component, "method_11023");
        if (!(arguments instanceof Object[])) arguments = new Object[0];
        try {
            Class<?> i18n = Class.forName("net.minecraft.class_1074");
            Object translated = callStatic(i18n, "method_4662", key, arguments);
            if (translated instanceof String && !((String) translated).isEmpty()
                && !key.equals(translated)) return (String) translated;
        } catch (Exception ignored) {}
        try {
            Class<?> languageType = Class.forName("net.minecraft.class_2477");
            Object language = callStatic(languageType, "method_10517");
            Object translated = call(language, "method_4679", key);
            if (translated instanceof String && !((String) translated).isEmpty()
                && !key.equals(translated)) return (String) translated;
        } catch (Exception ignored) {}
        return humanizeTranslationKey(key);
    }

    private static String resolveLegacyTranslationKey(String key) {
        if (key == null || key.isEmpty()) return null;
        try {
            Class<?> i18n = Class.forName("net.minecraft.class_1074");
            Object translated = callStatic(i18n, "method_4662", key, new Object[0]);
            if (translated instanceof String && !((String) translated).isEmpty()
                && !key.equals(translated)) return (String) translated;
        } catch (Exception ignored) {}
        try {
            Class<?> languageType = Class.forName("net.minecraft.class_2477");
            Object language = callStatic(languageType, "method_10517");
            Object translated = call(language, "method_4679", key);
            if (translated instanceof String && !((String) translated).isEmpty()
                && !key.equals(translated)) return (String) translated;
        } catch (Exception ignored) {}
        return null;
    }

    private static String humanizeTranslationKey(String key) {
        if (key == null) return null;
        int separator = key.lastIndexOf('.');
        String fallback = separator >= 0 ? key.substring(separator + 1) : key;
        fallback = fallback.replace('_', ' ').trim();
        return fallback.isEmpty() ? null : fallback;
    }

    static Object literal(String text) {
        // 1.16.x has a concrete LiteralText implementation. Prefer its constructor:
        // looking for an arbitrary static String factory on Text can select a method
        // whose result reports text through reflection but renders as an empty tooltip.
        try {
            Class<?> literal = Class.forName("net.minecraft.class_2585");
            Constructor<?> constructor = literal.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            Object legacy = constructor.newInstance(text);
            if (hasExactText(legacy, text)) return legacy;
        } catch (Exception ignored) {}
        try {
            Class<?> component = Class.forName("net.minecraft.class_2561");
            Object modern = callStatic(component, "method_43470", text);
            if (hasExactText(modern, text)) return modern;
            Object legacyFactory = callStatic(component, "method_30163", text);
            if (hasExactText(legacyFactory, text)) return legacyFactory;
            for (Method method : component.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0] == String.class && component.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    Object candidate = method.invoke(null, text);
                    if (hasExactText(candidate, text)) return candidate;
                }
            }
        } catch (Exception ignored) {}
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            Object official = callStatic(component, "literal", text);
            if (hasExactText(official, text)) return official;
        } catch (Exception ignored) {}
        return null;
    }

    static Object styledLiteral(String text, Object original) {
        Object result = literal(text);
        if (result == null || original == null) return result;
        try {
            Object style = call(original, "method_10866");
            if (style == null) style = call(original, "getStyle");
            for (Method method : original.getClass().getMethods()) {
                if (style != null) break;
                if (method.getParameterTypes().length == 0
                    && (method.getReturnType().getName().equals("net.minecraft.class_2583")
                    || method.getReturnType().getName().equals("net.minecraft.network.chat.Style"))) {
                    style = method.invoke(original);
                    break;
                }
            }
            if (style != null) {
                Object styled = call(result, "method_10862", style);
                if (styled == null) styled = call(result, "setStyle", style);
                if (styled != null && (hasClassInHierarchy(styled, "net.minecraft.class_2561")
                    || hasClassInHierarchy(styled, "net.minecraft.network.chat.Component"))
                    && hasExactText(styled, text)) return styled;
                for (Method method : result.getClass().getMethods()) {
                    if (method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isAssignableFrom(style.getClass())) {
                        method.setAccessible(true);
                        Object styledCandidate = method.invoke(result, style);
                        if (styledCandidate != null && hasClassInHierarchy(styledCandidate, "net.minecraft.class_2561")
                            && hasExactText(styledCandidate, text)) return styledCandidate;
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static boolean hasExactText(Object component, String expected) {
        if (component == null) return false;
        String actual = text(component);
        return expected == null ? actual == null : expected.equals(actual);
    }

    static void openMineTranslatorConfig(Object client) {
        if (client == null) return;
        Object parent = currentScreen(client);
        try {
            Class<?> configType = Class.forName("net.kingchoka.minetranslatorport.config.gui.ui.MineTranslatorConfigScreen");
            Constructor<?> constructor = null;
            for (Constructor<?> candidate : configType.getDeclaredConstructors()) {
                if (candidate.getParameterTypes().length == 1) {
                    constructor = candidate;
                    break;
                }
            }
            if (constructor == null) throw new NoSuchMethodException("MineTranslatorConfigScreen(Screen)");
            constructor.setAccessible(true);
            Object screen = constructor.newInstance(parent);
            try {
                invokeStrict(client, "method_1507", screen);
            } catch (NoSuchMethodException ignoredEx) {
                invokeStrict(client, "setScreen", screen);
            }
            return;
        } catch (ClassNotFoundException e) {
            try {
                Class<?> configType;
                try {
                    configType = Class.forName("net.kingchoka.minetranslatorport.LegacyConfigScreen26");
                } catch (ClassNotFoundException ex) {
                    configType = Class.forName("net.kingchoka.minetranslatorport.LegacyConfigScreen116");
                }
                Constructor<?> constructor = null;
                for (Constructor<?> candidate : configType.getDeclaredConstructors()) {
                    if (candidate.getParameterTypes().length == 1) {
                        constructor = candidate;
                        break;
                    }
                }
                if (constructor == null) throw new NoSuchMethodException("LegacyConfigScreen(Screen)");
                constructor.setAccessible(true);
                Object screen = constructor.newInstance(parent);
                try {
                    invokeStrict(client, "method_1507", screen);
                } catch (NoSuchMethodException ignoredEx) {
                    invokeStrict(client, "setScreen", screen);
                }
                return;
            } catch (Exception ignored) {}
        } catch (Exception exception) {
            restoreScreen(client, parent);
            System.out.println("[MineTranslator] Failed to open custom config:");
            exception.printStackTrace(System.out);
            return;
        }
        openControls(client);
    }

    static void addChat(Object chatHud, Object component) {
        Object result = call(chatHud, "method_1812", component);
        if (result == null) call(chatHud, "addMessage", component);
    }

    static void clearChat(Object chatHud) {
        Object result = call(chatHud, "method_1805", true);
        if (result == null) call(chatHud, "clearMessages", true);
    }

    static int hoveredChatIndex(Object client, Object chatHud, int available) {
        if (client == null || chatHud == null || available <= 0) return -1;
        try {
            Object window = call(client, "method_22683");
            Object mouse = findFieldValue(client, "net.minecraft.class_312");
            if (window == null) window = call(client, "getWindow");
            if (mouse == null) mouse = findFieldValue(client, "net.minecraft.client.MouseHandler");
            Number mouseX = (Number) call(mouse, "method_1603");
            Number mouseY = (Number) call(mouse, "method_1604");
            Number scaledHeight = (Number) call(window, "method_4502");
            Number scaledWidth = (Number) call(window, "method_4480");
            Number width = (Number) call(window, "method_4489");
            Number height = (Number) call(window, "method_4494");
            if (mouseX == null) mouseX = (Number) call(mouse, "xpos");
            if (mouseY == null) mouseY = (Number) call(mouse, "ypos");
            if (scaledHeight == null) scaledHeight = (Number) call(window, "getGuiScaledHeight");
            if (scaledWidth == null) scaledWidth = (Number) call(window, "getGuiScaledWidth");
            if (scaledHeight == null) scaledHeight = (Number) call(window, "method_4486");
            if (width == null) width = (Number) call(window, "getWidth");
            if (height == null) height = (Number) call(window, "getHeight");
            if (mouseY == null || scaledHeight == null || height == null || height.doubleValue() == 0) return 0;
            double x = mouseX != null && scaledWidth != null && width != null && width.doubleValue() != 0
                ? mouseX.doubleValue() * scaledWidth.doubleValue() / width.doubleValue()
                : 0.0;
            double y = mouseY.doubleValue() * scaledHeight.doubleValue() / height.doubleValue();

            // 1.19.2 ChatHud already contains the exact chat-scale, line-spacing and
            // scroll-aware coordinate conversion. Using it avoids selecting another
            // message when GUI scale or chat spacing differs from the defaults.
            Object transformedX = call(chatHud, "method_44722", x);
            Object transformedY = call(chatHud, "method_44724", y);
            if (transformedY instanceof Number) {
                Object visibleIndex = transformedX instanceof Number
                    ? call(chatHud, "method_44725", ((Number) transformedX).doubleValue(), ((Number) transformedY).doubleValue())
                    : null;
                if (!(visibleIndex instanceof Number)) {
                    visibleIndex = call(chatHud, "method_44725", ((Number) transformedY).doubleValue());
                }
                if (visibleIndex instanceof Number && ((Number) visibleIndex).intValue() >= 0) {
                    return Math.min(((Number) visibleIndex).intValue(), available - 1);
                }
            }

            int index = (int) Math.floor((scaledHeight.doubleValue() - 40.0 - y) / 9.0);
            if (index < 0) index = 0;
            return Math.min(index, available - 1);
        } catch (Exception ignored) {
            return 0;
        }
    }

    static Object call(Object target, String name, Object... args) {
        if (target == null) return null;
        Class<?> current = target.getClass();
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && compatible(method.getParameterTypes(), args)) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target, args);
                    } catch (Exception ignored) {}
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Object callPublic(Object target, String name, Object... args) {
        if (target == null) return null;
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && compatible(method.getParameterTypes(), args)) {
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Object invokeStrict(Object target, String name, Object... args) throws Exception {
        if (target == null) throw new IllegalArgumentException("target");
        Class<?> current = target.getClass();
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && compatible(method.getParameterTypes(), args)) {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static Object callStatic(Class<?> type, String name, Object... args) {
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getName().equals(name)
                && compatible(method.getParameterTypes(), args)) {
                try {
                    method.setAccessible(true);
                    return method.invoke(null, args);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Object findFieldValue(Object owner, String wantedClass) {
        if (owner == null) return null;
        Class<?> current = owner.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value != null && hasClassInHierarchy(value, wantedClass)) return value;
                } catch (Exception ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field[] allFields(Class<?> type) {
        Field[] result = new Field[0];
        Class<?> current = type;
        while (current != null) {
            Field[] declared = current.getDeclaredFields();
            int oldLength = result.length;
            result = Arrays.copyOf(result, oldLength + declared.length);
            System.arraycopy(declared, 0, result, oldLength, declared.length);
            current = current.getSuperclass();
        }
        return result;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        return null;
    }

    private static boolean isRecord(Class<?> clazz) {
        try {
            Method isRecordMethod = Class.class.getMethod("isRecord");
            return (Boolean) isRecordMethod.invoke(clazz);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object getRecordFieldValue(Object entry, Field field) throws Exception {
        Method accessor = entry.getClass().getMethod(field.getName());
        return accessor.invoke(entry);
    }

    private static Object cloneAndReplaceRecordField(Object entry, Field targetField, Object newValue) throws Exception {
        Class<?> clazz = entry.getClass();
        Object[] recordComponents = (Object[]) Class.class.getMethod("getRecordComponents").invoke(clazz);
        Class<?>[] paramTypes = new Class<?>[recordComponents.length];
        Object[] args = new Object[recordComponents.length];
        
        for (int i = 0; i < recordComponents.length; i++) {
            Object comp = recordComponents[i];
            String name = (String) comp.getClass().getMethod("getName").invoke(comp);
            Class<?> type = (Class<?>) comp.getClass().getMethod("getType").invoke(comp);
            Method accessor = (Method) comp.getClass().getMethod("getAccessor").invoke(comp);
            
            paramTypes[i] = type;
            if (name.equals(targetField.getName())) {
                args[i] = newValue;
            } else {
                args[i] = accessor.invoke(entry);
            }
        }
        
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }

    @SuppressWarnings("unchecked")
    static void replaceMessageInChat(Object chatHud, String originalText, Object translatedComponent) {
        if (chatHud == null || originalText == null || translatedComponent == null) return;
        try {
            boolean found = false;
            for (Field field : chatHud.getClass().getDeclaredFields()) {
                if (field.getType() != List.class) continue;
                field.setAccessible(true);
                List<Object> messages = (List<Object>) field.get(chatHud);
                if (messages == null || messages.isEmpty()) continue;

                for (int i = messages.size() - 1; i >= 0 && !found; i--) {
                    Object entry = messages.get(i);
                    if (entry == null) continue;

                    if (entry.getClass().getName().startsWith("java.")) {
                        if (entry instanceof String && entry.equals(originalText)) {
                            messages.set(i, text(translatedComponent));
                            found = true;
                        }
                        continue;
                    }

                    Field compField = null;
                    boolean record = isRecord(entry.getClass());
                    
                    for (Field candidate : allFields(entry.getClass())) {
                        if (Modifier.isStatic(candidate.getModifiers())) continue;
                        
                        Object value;
                        if (record) {
                            try {
                                value = getRecordFieldValue(entry, candidate);
                            } catch (Exception ignored) {
                                continue;
                            }
                        } else {
                            candidate.setAccessible(true);
                            value = candidate.get(entry);
                        }
                        
                        if (value == null) continue;
                        if (!hasClassInHierarchy(candidate.getType(), "net.minecraft.class_2561")
                            && !hasClassInHierarchy(candidate.getType(), "net.minecraft.network.chat.Component")
                            && !hasClassInHierarchy(value, "net.minecraft.class_2561")
                            && !hasClassInHierarchy(value, "net.minecraft.network.chat.Component")) {
                            continue;
                        }
                        if (originalText.equals(text(value))) {
                            compField = candidate;
                            break;
                        }
                    }

                    if (compField == null) continue;
                    
                    if (record) {
                        Object replacement = cloneAndReplaceRecordField(entry, compField, translatedComponent);
                        messages.set(i, replacement);
                    } else {
                        compField.setAccessible(true);
                        compField.set(entry, translatedComponent);
                    }
                    found = true;
                }
                if (found) break;
            }

            if (found) {
                refreshVisibleChat(chatHud);
                System.out.println("[MineTranslator] Chat line translated and refreshed.");
            } else {
                System.out.println("[MineTranslator] Chat line was no longer available for replacement.");
            }
        } catch (Exception e) {
            System.out.println("[MineTranslator] Failed to replace chat line: "
                + rootCause(e).getClass().getSimpleName());
            rootCause(e).printStackTrace();
        }
    }

    private static void refreshVisibleChat(Object chatHud) throws Exception {
        // Minecraft 1.19.2: ChatHud.method_44813 clears visible OrderedText rows and
        // rebuilds them from the signed-message history. method_44811 adds a message
        // and method_1808 clears the chat, so neither is a valid refresh operation.
        String[] refreshMethods = {"method_44813", "method_1817", "refreshTrimmedMessages", "refreshTrimmedMessage"};
        NoSuchMethodException missing = null;
        for (String method : refreshMethods) {
            try {
                invokeStrict(chatHud, method);
                return;
            } catch (NoSuchMethodException exception) {
                missing = exception;
            }
        }
        if (missing != null) throw missing;
    }

    private static boolean hasClassInHierarchy(Object value, String className) {
        if (value == null) return false;
        Class<?> current = value instanceof Class ? (Class<?>) value : value.getClass();
        while (current != null) {
            if (current.getName().equals(className)) return true;
            for (Class<?> iface : current.getInterfaces()) if (iface.getName().equals(className)) return true;
            current = current.getSuperclass();
        }
        return false;
    }

    private static boolean compatible(Class<?>[] parameters, Object[] args) {
        if (parameters.length != args.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            if (args[i] == null) continue;
            Class<?> parameter = wrap(parameters[i]);
            if (!parameter.isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
