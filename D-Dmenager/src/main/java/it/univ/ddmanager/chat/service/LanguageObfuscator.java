package it.univ.ddmanager.chat.service;

import it.univ.ddmanager.character.model.CharacterLanguage;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LanguageObfuscator {

    private static final String ALPHABET_GENERIC_RUNES = "ᚠᚢᚦᚨᚱᚲᚷᚺᚾᛁᛃᛇᛈᛉᛋᛏᛒᛗᛚᛝ";
    private static final String ALPHABET_ELVISH = "ᚫᚬᚮᚯᚱᚻᚼᛁᛂᛃᛇᛉᛋᛏᛗᛚᛝᛟᛠᛡᛣᛤ";
    private static final String ALPHABET_DRACONIC = "ΔΨΞЖФЏѪѬҚҰҖҜҨҴӁӜӞӦӪҔҖҘҞ";
    private static final String ALPHABET_EGYPTIAN = "☥◈◉✢✶✷✸✹✺✻✼✽✾✿❂❈❉❊❋◬◭◮";
    private static final String ALPHABET_DEMONIC = "☠☡☣☤☥☦☧☨☩☪☫☬☭☮☯☸☺☻☼☽☾☿♀♁♂♃♄♅♆♇♈♉♊♋♌♍♎♏♐♑♒♓";
    private static final Set<Character> PRESERVED_PUNCTUATION = Set.of(
            '.', ',', '!', '?', ';', ':', '\'', '"', '(', ')', '[', ']', '{', '}', '-', '_');

    public String encode(CharacterLanguage language, String contentRaw) {
        if (contentRaw == null || contentRaw.isEmpty()) {
            return "";
        }
        String alphabet = getAlphabetForLanguage(language);
        StringBuilder builder = new StringBuilder(contentRaw.length());
        for (int i = 0; i < contentRaw.length(); i++) {
            char raw = contentRaw.charAt(i);
            if (Character.isWhitespace(raw)) {
                builder.append(raw);
                continue;
            }
            if (PRESERVED_PUNCTUATION.contains(raw)) {
                builder.append(raw);
                continue;
            }
            if (Character.isLetterOrDigit(raw)) {
                int idx = Math.floorMod(raw + i * 31, alphabet.length());
                builder.append(alphabet.charAt(idx));
            } else {
                int idx = Math.floorMod(raw + i * 17, alphabet.length());
                builder.append(alphabet.charAt(idx));
            }
        }
        return builder.toString();
    }

    private String getAlphabetForLanguage(CharacterLanguage language) {
        if (language == null) {
            return ALPHABET_GENERIC_RUNES;
        }
        return switch (language) {
            case ELVISH, CELESTIAL, SYLVAN -> ALPHABET_ELVISH;
            case DRACONIC, PRIMORDIAL -> ALPHABET_DRACONIC;
            case EGYPTIAN -> ALPHABET_EGYPTIAN;
            case ORC, GOBLIN, INFERNAL, ABYSSAL -> ALPHABET_DEMONIC;
            case GIANT, DWARVISH, COMMON -> ALPHABET_GENERIC_RUNES;
            default -> ALPHABET_GENERIC_RUNES;
        };
    }
}
