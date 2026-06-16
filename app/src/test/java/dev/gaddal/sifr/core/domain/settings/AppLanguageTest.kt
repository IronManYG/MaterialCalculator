package dev.gaddal.sifr.core.domain.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLanguageTest {
    @Test fun `default language is System`() {
        assertThat(AppSettings().language).isEqualTo(AppLanguage.System)
    }

    @Test fun `every language name round-trips through valueOf`() {
        AppLanguage.entries.forEach { l ->
            assertThat(AppLanguage.valueOf(l.name)).isEqualTo(l)
        }
    }

    @Test fun `tags are null, en, ar`() {
        assertThat(AppLanguage.System.tag).isNull()
        assertThat(AppLanguage.English.tag).isEqualTo("en")
        assertThat(AppLanguage.Arabic.tag).isEqualTo("ar")
    }

    @Test fun `tier-1 Latin language tags are valid BCP-47`() {
        assertThat(AppLanguage.Spanish.tag).isEqualTo("es")
        assertThat(AppLanguage.Portuguese.tag).isEqualTo("pt-BR")
        assertThat(AppLanguage.French.tag).isEqualTo("fr")
        assertThat(AppLanguage.German.tag).isEqualTo("de")
        assertThat(AppLanguage.Indonesian.tag).isEqualTo("id")
        assertThat(AppLanguage.Turkish.tag).isEqualTo("tr")
        assertThat(AppLanguage.Italian.tag).isEqualTo("it")
        assertThat(AppLanguage.Vietnamese.tag).isEqualTo("vi")
    }

    @Test fun `Russian (Cyrillic) tag is valid BCP-47`() {
        assertThat(AppLanguage.Russian.tag).isEqualTo("ru")
    }

    @Test fun `every non-System language carries a non-null tag`() {
        // System follows the device locale (no tag of its own); every other language
        // must carry a tag so SifrLocale can resolve a Locale + a values-<tag> folder.
        AppLanguage.entries.filter { it != AppLanguage.System }.forEach { l ->
            assertThat(l.tag).isNotNull()
        }
    }
}
