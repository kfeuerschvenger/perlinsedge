package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightRegistryTest {

    private static PointLight light(double x) {
        return new PointLight(x, 0, 3.0, new Rgba(1, 0.7, 0.4, 1), 1.0, Flicker.STEADY);
    }

    @Test
    void aNewRegistryIsEmpty() {
        final LightRegistry registry = new LightRegistry();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    void aSourceIsAddedOnceAndRead() {
        final LightRegistry registry = new LightRegistry();
        final PointLight torch = light(1);

        assertTrue(registry.add(torch));
        assertFalse(registry.add(torch), "the same source is not registered twice");

        assertEquals(1, registry.size());
        assertTrue(registry.contains(torch));
        assertSame(torch, registry.get(0));
    }

    @Test
    void equalSourcesThatAreDifferentObjectsAreBothKept() {
        final LightRegistry registry = new LightRegistry();

        assertTrue(registry.add(light(1)));
        assertTrue(registry.add(light(1)));

        assertEquals(2, registry.size());
    }

    @Test
    void aSourceIsRemovedWithoutDisturbingTheOthersAndTheirOrder() {
        final LightRegistry registry = new LightRegistry();
        final PointLight a = light(1);
        final PointLight b = light(2);
        final PointLight c = light(3);
        registry.add(a);
        registry.add(b);
        registry.add(c);

        assertTrue(registry.remove(b));
        assertFalse(registry.remove(b), "it is gone");

        assertEquals(2, registry.size());
        assertSame(a, registry.get(0));
        assertSame(c, registry.get(1));
        assertFalse(registry.contains(b));
    }

    @Test
    void clearTakesEverySourceOut() {
        final LightRegistry registry = new LightRegistry();
        registry.add(light(1));
        registry.add(light(2));

        registry.clear();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
        assertTrue(registry.add(light(3)), "a cleared registry takes sources again");
    }

    @Test
    void walkingTheRegistryByIndexHandsBackTheRegisteredInstances() {
        final LightRegistry registry = new LightRegistry();
        final PointLight[] added = new PointLight[20];
        for (int i = 0; i < added.length; i++) {
            added[i] = light(i);
            registry.add(added[i]);
        }

        // No iterator and no copy: the very objects come back, so a frame allocates nothing to read them
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0, count = registry.size(); i < count; i++) {
                assertSame(added[i], registry.get(i));
            }
        }
    }

    @Test
    void aNullSourceIsRefused() {
        final LightRegistry registry = new LightRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.add(null));
        assertFalse(registry.remove(null));
    }

    @Test
    void aPointLightRefusesValuesThatCannotLight() {
        final Rgba colour = new Rgba(1, 1, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new PointLight(0, 0, 0.0, colour, 1.0, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new PointLight(0, 0, 2.0, colour, 1.5, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new PointLight(0, 0, 2.0, null, 1.0, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new PointLight(0, 0, 2.0, colour, 1.0, null));
    }
}
