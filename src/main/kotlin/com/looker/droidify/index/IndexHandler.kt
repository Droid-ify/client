package com.looker.droidify.index

import com.looker.droidify.entity.Product
import com.looker.droidify.entity.Release
import com.looker.droidify.utility.extension.android.*
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class IndexHandler(private val repositoryId: Long, private val callback: Callback): DefaultHandler() {
  companion object {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      .apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun String.parseDate(): Long {
      return try {
        dateFormat.parse(this)?.time ?: 0L
      } catch (e: Exception) {
        0L
      }
    }

    internal fun validateIcon(icon: String): String {
      return if (icon.endsWith(".xml")) "" else icon
    }
  }

  interface Callback {
    fun onRepository(mirrors: List<String>, name: String, description: String,
      certificate: String, version: Int, timestamp: Long)
    fun onProduct(product: Product)
  }

  internal object DonateComparator: Comparator<Product.Donate> {
    private val classes = listOf(Product.Donate.Regular::class, Product.Donate.Bitcoin::class,
      Product.Donate.Litecoin::class, Product.Donate.Flattr::class, Product.Donate.Liberapay::class,
      Product.Donate.OpenCollective::class)

    override fun compare(donate1: Product.Donate, donate2: Product.Donate): Int {
      val index1 = classes.indexOf(donate1::class)
      val index2 = classes.indexOf(donate2::class)
      return when {
        index1 >= 0 && index2 == -1 -> -1
        index2 >= 0 && index1 == -1 -> 1
        else -> index1.compareTo(index2)
      }
    }
  }

  private class RepositoryBuilder {
    var address = ""
    val mirrors = mutableListOf<String>()
    var name = ""
    var description = ""
    var certificate = ""
    var version = -1
    var timestamp = 0L
  }

  private class ProductBuilder(val repositoryId: Long, val packageName: String) {
    var name = ""
    var summary = ""
    var description = ""
    var icon = ""
    var authorName = ""
    var authorEmail = ""
    var source = ""
    var changelog = ""
    var web = ""
    var tracker = ""
    var added = 0L
    var updated = 0L
    var suggestedVersionCode = 0L
    val categories = linkedSetOf<String>()
    val antiFeatures = linkedSetOf<String>()
    val licenses = mutableListOf<String>()
    val donates = mutableListOf<Product.Donate>()
    val releases = mutableListOf<Release>()

    fun build(): Product {
      return Product(repositoryId, packageName, name, summary, description, "", icon, "",
        Product.Author(authorName, authorEmail, ""), source, changelog, web, tracker, added, updated,
        suggestedVersionCode, categories.toList(), antiFeatures.toList(),
        licenses, donates.sortedWith(DonateComparator), emptyList(), releases)
    }
  }

  private class ReleaseBuilder {
    var version = ""
    var versionCode = 0L
    var added = 0L
    var size = 0L
    var minSdkVersion = 0
    var targetSdkVersion = 0
    var maxSdkVersion = 0
    var source = ""
    var release = ""
    var hash = ""
    var hashType = ""
    var signature = ""
    var obbMain = ""
    var obbMainHash = ""
    var obbPatch = ""
    var obbPatchHash = ""
    val permissions = linkedSetOf<String>()
    val features = linkedSetOf<String>()
    val platforms = linkedSetOf<String>()

    fun build(): Release {
      val hashType = if (hash.isNotEmpty() && hashType.isEmpty()) "sha256" else hashType
      val obbMainHashType = if (obbMainHash.isNotEmpty()) "sha256" else ""
      val obbPatchHashType = if (obbPatchHash.isNotEmpty()) "sha256" else ""
      return Release(false, version, versionCode, added, size,
        minSdkVersion, targetSdkVersion, maxSdkVersion, source, release, hash, hashType, signature,
        obbMain, obbMainHash, obbMainHashType, obbPatch, obbPatchHash, obbPatchHashType,
        permissions.toList(), features.toList(), platforms.toList(), emptyList())
    }
  }

  private val contentBuilder = StringBuilder()

  private var repositoryBuilder: RepositoryBuilder? = RepositoryBuilder()
  private var productBuilder: ProductBuilder? = null
  private var releaseBuilder: ReleaseBuilder? = null

  private fun Attributes.get(localName: String): String = getValue("", localName).orEmpty()
  private fun String.cleanWhiteSpace(): String = replace("\\s".toRegex(), " ")

  override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
    super.startElement(uri, localName, qName, attributes)

    val repositoryBuilder = repositoryBuilder
    val productBuilder = productBuilder
    val releaseBuilder = releaseBuilder
    contentBuilder.setLength(0)

    when {
      localName == "repo" -> {
        if (repositoryBuilder != null) {
          repositoryBuilder.address = attributes.get("url").cleanWhiteSpace()
          repositoryBuilder.name = attributes.get("name").cleanWhiteSpace()
          repositoryBuilder.description = attributes.get("description").cleanWhiteSpace()
          repositoryBuilder.certificate = attributes.get("pubkey")
          repositoryBuilder.version = attributes.get("version").toIntOrNull() ?: 0
          repositoryBuilder.timestamp = (attributes.get("timestamp").toLongOrNull() ?: 0L) * 1000L
        }
      }
      localName == "application" && productBuilder == null -> {
        this.productBuilder = ProductBuilder(repositoryId, attributes.get("id"))
      }
      localName == "package" && productBuilder != null && releaseBuilder == null -> {
        this.releaseBuilder = ReleaseBuilder()
      }
      localName == "hash" && releaseBuilder != null -> {
        releaseBuilder.hashType = attributes.get("type")
      }
      (localName == "uses-permission" || localName.startsWith("uses-permission-")) && releaseBuilder != null -> {
        val minSdkVersion = if (localName != "uses-permission") {
          "uses-permission-sdk-(\\d+)".toRegex().matchEntire(localName)
            ?.destructured?.let { (version) -> version.toIntOrNull() }
        } else {
          null
        } ?: 0
        val maxSdkVersion = attributes.get("maxSdkVersion").toIntOrNull() ?: Int.MAX_VALUE
        if (Android.sdk in minSdkVersion .. maxSdkVersion) {
          releaseBuilder.permissions.add(attributes.get("name"))
        } else {
          releaseBuilder.permissions.remove(attributes.get("name"))
        }
      }
    }
  }

  override fun endElement(uri: String, localName: String, qName: String) {
    super.endElement(uri, localName, qName)

    val repositoryBuilder = repositoryBuilder
    val productBuilder = productBuilder
    val releaseBuilder = releaseBuilder
    val content = contentBuilder.toString()

    when {
      localName == "repo" -> {
        if (repositoryBuilder != null) {
          val mirrors = (listOf(repositoryBuilder.address) + repositoryBuilder.mirrors)
            .filter { it.isNotEmpty() }.distinct()
          callback.onRepository(mirrors, repositoryBuilder.name, repositoryBuilder.description,
            repositoryBuilder.certificate, repositoryBuilder.version, repositoryBuilder.timestamp)
          this.repositoryBuilder = null
        }
      }
      localName == "application" && productBuilder != null -> {
        val product = productBuilder.build()
        this.productBuilder = null
        callback.onProduct(product)
      }
      localName == "package" && productBuilder != null && releaseBuilder != null -> {
        productBuilder.releases.add(releaseBuilder.build())
        this.releaseBuilder = null
      }
      repositoryBuilder != null -> {
        when (localName) {
          "description" -> repositoryBuilder.description = content.cleanWhiteSpace()
          "mirror" -> repositoryBuilder.mirrors += content
        }
      }
      productBuilder != null && releaseBuilder != null -> {
        when (localName) {
          "version" -> releaseBuilder.version = content
          "versioncode" -> releaseBuilder.versionCode = content.toLongOrNull() ?: 0L
          "added" -> releaseBuilder.added = content.parseDate()
          "size" -> releaseBuilder.size = content.toLongOrNull() ?: 0
          "sdkver" -> releaseBuilder.minSdkVersion = content.toIntOrNull() ?: 0
          "targetSdkVersion" -> releaseBuilder.targetSdkVersion = content.toIntOrNull() ?: 0
          "maxsdkver" -> releaseBuilder.maxSdkVersion = content.toIntOrNull() ?: 0
          "srcname" -> releaseBuilder.source = content
          "apkname" -> releaseBuilder.release = content
          "hash" -> releaseBuilder.hash = content
          "sig" -> releaseBuilder.signature = content
          "obbMainFile" -> releaseBuilder.obbMain = content
          "obbMainFileSha256" -> releaseBuilder.obbMainHash = content
          "obbPatchFile" -> releaseBuilder.obbPatch = content
          "obbPatchFileSha256" -> releaseBuilder.obbPatchHash = content
          "permissions" -> releaseBuilder.permissions += content.split(',').filter { it.isNotEmpty() }
          "features" -> releaseBuilder.features += content.split(',').filter { it.isNotEmpty() }
          "nativecode" -> releaseBuilder.platforms += content.split(',').filter { it.isNotEmpty() }
        }
      }
      productBuilder != null -> {
        when (localName) {
          "name" -> productBuilder.name = content
          "summary" -> productBuilder.summary = content
          "description" -> productBuilder.description = "<p>$content</p>"
          "desc" -> productBuilder.description = content.replace("\n", "<br/>")
          "icon" -> productBuilder.icon = validateIcon(content)
          "author" -> productBuilder.authorName = content
          "email" -> productBuilder.authorEmail = content
          "source" -> productBuilder.source = content
          "changelog" -> productBuilder.changelog = content
          "web" -> productBuilder.web = content
          "tracker" -> productBuilder.tracker = content
          "added" -> productBuilder.added = content.parseDate()
          "lastupdated" -> productBuilder.updated = content.parseDate()
          "marketvercode" -> productBuilder.suggestedVersionCode = content.toLongOrNull() ?: 0L
          "categories" -> productBuilder.categories += content.split(',').filter { it.isNotEmpty() }
          "antifeatures" -> productBuilder.antiFeatures += content.split(',').filter { it.isNotEmpty() }
          "license" -> productBuilder.licenses += content.split(',').filter { it.isNotEmpty() }
          "donate" -> productBuilder.donates += Product.Donate.Regular(content)
          "bitcoin" -> productBuilder.donates += Product.Donate.Bitcoin(content)
          "litecoin" -> productBuilder.donates += Product.Donate.Litecoin(content)
          "flattr" -> productBuilder.donates += Product.Donate.Flattr(content)
          "liberapay" -> productBuilder.donates += Product.Donate.Liberapay(content)
          "openCollective" -> productBuilder.donates += Product.Donate.OpenCollective(content)
        }
      }
    }
  }

  override fun characters(ch: CharArray, start: Int, length: Int) {
    super.characters(ch, start, length)
    contentBuilder.append(ch, start, length)
  }
}
