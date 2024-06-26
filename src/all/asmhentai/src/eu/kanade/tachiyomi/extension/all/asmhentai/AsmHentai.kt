package eu.kanade.tachiyomi.extension.all.asmhentai

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.cleanTag
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.FormBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AsmHentai(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "AsmHentai",
    "https://asmhentai.com",
    lang = lang,
) {
    override val supportsLatest = mangaLang.isNotBlank()

    override fun Element.mangaUrl() =
        selectFirst(".image a")?.attr("abs:href")

    override fun Element.mangaThumbnail() =
        selectFirst(".image img")?.imgAttr()

    override fun Element.mangaLang() =
        select("a:has(.flag)").attr("href")
            .removeSuffix("/").substringAfterLast("/")

    override fun popularMangaSelector() = ".preview_item"

    override fun Element.getInfo(tag: String): String {
        return select(".tags:contains($tag:) .tag")
            .joinToString { it.ownText().cleanTag() }
    }

    override fun Element.getDescription(): String {
        return (
            listOf("Parodies", "Characters", "Languages", "Category")
                .mapNotNull { tag ->
                    getInfo(tag)
                        .let { if (it.isNotBlank()) "$tag: $it" else null }
                } +
                listOfNotNull(
                    selectFirst(".book_page .pages h3")?.ownText(),
                    selectFirst(".book_page h1 + h2")?.ownText()
                        .let { altTitle -> if (!altTitle.isNullOrBlank()) "Alternate Title: $altTitle" else null },
                )
            )
            .joinToString("\n\n")
            .plus(
                if (preferences.shortTitle) {
                    "\nFull title: ${mangaFullTitle("h1")}"
                } else {
                    ""
                },
            )
    }

    /* Search */
    override val favoritePath = "inc/user.php?act=favs"

    override val mangaDetailInfoSelector = ".book_page"

    override val galleryIdSelector = "load_id"
    override val totalPagesSelector = "t_pages"
    override val pageUri = "gallery"
    override val pageSelector = ".preview_thumb"

    override fun pageRequestForm(document: Document, totalPages: String): FormBody {
        val token = document.select("[name=csrf-token]").attr("content")

        return FormBody.Builder()
            .add("_token", token)
            .add("id", document.inputIdValueOf(loadIdSelector))
            .add("dir", document.inputIdValueOf(loadDirSelector))
            .add("visible_pages", "10")
            .add("t_pages", totalPages)
            .add("type", "2") // 1 would be "more", 2 is "all remaining"
            .build()
    }

    /* Filters */
    override fun tagsParser(document: Document): List<Pair<String, String>> {
        return document.select(".tags_page ul.tags li")
            .mapNotNull {
                Pair(
                    it.selectFirst("a.tag")?.ownText() ?: "",
                    it.select("a.tag").attr("href")
                        .removeSuffix("/").substringAfterLast('/'),
                )
            }
    }

    override fun getFilterList() = FilterList(
        listOf(
            Filter.Header("HINT: Separate search term with comma (,)"),
        ) + super.getFilterList().list,
    )
}
