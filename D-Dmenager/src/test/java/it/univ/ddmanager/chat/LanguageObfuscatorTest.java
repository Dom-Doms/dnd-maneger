package it.univ.ddmanager.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.univ.ddmanager.character.model.CharacterLanguage;
import it.univ.ddmanager.chat.service.LanguageObfuscator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class LanguageObfuscatorTest {

    private final LanguageObfuscator obfuscator = new LanguageObfuscator();

    @Test
    void elvishEncodingUsesElvishAlphabet() {
        String encoded = obfuscator.encode(CharacterLanguage.ELVISH, "HelloWorld");
        assertNotEquals("HelloWorld", encoded);
        String alphabet = alphabetFor(CharacterLanguage.ELVISH);
        assertTrue(encoded.chars().allMatch(ch -> alphabet.indexOf(ch) >= 0));
    }

    @Test
    void draconicEncodingDiffersFromElvish() {
        String draconic = obfuscator.encode(CharacterLanguage.DRACONIC, "HelloWorld");
        String elvish = obfuscator.encode(CharacterLanguage.ELVISH, "HelloWorld");
        assertNotEquals(elvish, draconic);
        String alphabet = alphabetFor(CharacterLanguage.DRACONIC);
        assertTrue(draconic.chars().allMatch(ch -> alphabet.indexOf(ch) >= 0));
    }

    @Test
    void egyptianEncodingUsesGeometricSymbols() {
        String encoded = obfuscator.encode(CharacterLanguage.EGYPTIAN, "Magic");
        assertNotEquals("Magic", encoded);
        String alphabet = alphabetFor(CharacterLanguage.EGYPTIAN);
        assertTrue(encoded.chars().allMatch(ch -> alphabet.indexOf(ch) >= 0));
    }

    @Test
    void preservesWhitespaceAndPunctuation() {
        String encoded = obfuscator.encode(CharacterLanguage.ELVISH, "Hi, brave world!");
        char[] original = "Hi, brave world!".toCharArray();
        for (int i = 0; i < original.length; i++) {
            char originalChar = original[i];
            char encodedChar = encoded.charAt(i);
            if (Character.isWhitespace(originalChar) || ",.!?;:".indexOf(originalChar) >= 0) {
                assertEquals(originalChar, encodedChar, "Expected punctuation preserved at index " + i);
            }
        }
    }

    @Test
    void encodingIsDeterministic() {
        String first = obfuscator.encode(CharacterLanguage.DRACONIC, "Ancient secret");
        String second = obfuscator.encode(CharacterLanguage.DRACONIC, "Ancient secret");
        assertEquals(first, second);
    }

    private String alphabetFor(CharacterLanguage language) {
        try {
            Method m = LanguageObfuscator.class.getDeclaredMethod("getAlphabetForLanguage", CharacterLanguage.class);
            m.setAccessible(true);
            return (String) m.invoke(obfuscator, language);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            throw new IllegalStateException("Unable to access alphabet", ex);
        }
    }
}
