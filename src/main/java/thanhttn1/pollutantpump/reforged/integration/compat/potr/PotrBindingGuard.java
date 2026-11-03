/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Added in 1.1.0 (2026-11-02): startup check of the Pollution of the Realms members this mod calls.
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat.potr;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import net.neoforged.fml.ModList;

/**
 * Pollution of the Realms (mod id {@code adpother}) has no public API, and its version range here is open ended. The
 * class literals preloaded by {@code PollutantSuction.preload()} only prove that the classes exist; a renamed
 * <em>method</em> would still crash in the middle of a pump tick with a {@link NoSuchMethodError}. This guard looks
 * the members up by name and parameter count (without initialising any class) while the game is still loading, so
 * the problem is reported once, early, with a readable message.
 *
 * <p>It only uses reflection on names, so it never links against the other mod and cannot itself fail with a
 * linkage error.
 */
public final class PotrBindingGuard {

    private static final String POTR_MOD_ID = "adpother";
    private static final String BLOCKS = "com.endertech.minecraft.mods.adpother.blocks.";

    private PotrBindingGuard() {
    }

    /**
     * @param strict when true a mismatch throws {@link IllegalStateException}, otherwise it is only logged
     */
    public static void verify(Logger logger, boolean strict) {
        String version = ModList.get().getModContainerById(POTR_MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
        List<String> problems = new ArrayList<>();
        ClassLoader loader = PotrBindingGuard.class.getClassLoader();

        Class<?> gas = load(loader, BLOCKS + "AbstractGas", problems);
        Class<?> filterFrame = load(loader, BLOCKS + "FilterFrame", problems);
        load(loader, BLOCKS + "FilterFrame$BlockTile", problems);

        if (gas != null) {
            requireMethod(gas, "getCarriedPollutionAmount", 1, problems);
        }
        if (filterFrame != null) {
            requireMethod(filterFrame, "fill", 3, problems);
            Method getContent = findMethod(filterFrame, "getContent", 1);
            if (getContent == null) {
                problems.add("FilterFrame.getContent(1 parameter) is missing");
            } else {
                requireMethod(getContent.getReturnType(), "getFreeSpaceFor", 1, problems);
            }
        }

        if (problems.isEmpty()) {
            logger.debug("Pollution of the Realms {} matches the members this mod calls", version);
            return;
        }
        String message = "Pollution of the Realms " + version + " is not compatible with this build of Pollutant Pump: "
                + String.join("; ", problems) + ". Install a Pollutant Pump build made for that version"
                + " (or set strictDependencyCheck=false in the config to ignore this at your own risk).";
        if (strict) {
            throw new IllegalStateException(message);
        }
        logger.error(message);
    }

    private static Class<?> load(ClassLoader loader, String name, List<String> problems) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            problems.add("class " + name + " is missing");
            return null;
        }
    }

    private static void requireMethod(Class<?> owner, String name, int parameterCount, List<String> problems) {
        if (findMethod(owner, name, parameterCount) == null) {
            problems.add(owner.getSimpleName() + "." + name + "(" + parameterCount + " parameter"
                    + (parameterCount == 1 ? "" : "s") + ") is missing");
        }
    }

    /** Searches the class, its superclasses and its interfaces, including non-public methods. */
    private static Method findMethod(Class<?> owner, String name, int parameterCount) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            Method found = scan(type, name, parameterCount);
            if (found != null) {
                return found;
            }
            for (Class<?> iface : type.getInterfaces()) {
                found = scan(iface, name, parameterCount);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Method scan(Class<?> type, String name, int parameterCount) {
        try {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isBridge() && method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        } catch (LinkageError ignored) {
            // A member type of the other mod could not be resolved; treat as not found.
        }
        return null;
    }
}
