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
}
