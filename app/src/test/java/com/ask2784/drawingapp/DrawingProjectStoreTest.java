package com.ask2784.drawingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingProjectStoreTest {
    @Test
    public void nullAndBlankNamesAreRejected() {
        assertEquals("", DrawingProjectStore.sanitizeName(null));
        assertEquals("", DrawingProjectStore.sanitizeName("   "));
        assertEquals("", DrawingProjectStore.sanitizeName("...."));
    }

    @Test
    public void projectNameCannotEscapeProjectDirectory() {
        String safe = DrawingProjectStore.sanitizeName("../../private/drawing");
        assertNotNull(safe);
        assertFalse(safe.isEmpty());
        assertFalse(safe.contains("/"));
        assertFalse(safe.contains("\\"));
        assertFalse(safe.startsWith("."));
    }

    @Test
    public void projectNameLengthIsBounded() {
        String safe = DrawingProjectStore.sanitizeName("a".repeat(200));
        assertEquals(64, safe.length());
        assertTrue(safe.matches("[A-Za-z0-9 _.-]+"));
    }

    @Test
    public void unsupportedCharactersAreNormalized() {
        String safe = DrawingProjectStore.sanitizeName("drawing: photo/签名");
        assertFalse(safe.contains(":"));
        assertFalse(safe.contains("/"));
        assertTrue(safe.contains("_"));
    }
}
