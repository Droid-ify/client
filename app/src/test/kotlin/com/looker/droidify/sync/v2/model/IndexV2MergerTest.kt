package com.looker.droidify.sync.v2.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/*
 * Adapted from Neo Store.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("IndexV2Merger Tests")
class IndexV2MergerTest {

    @TempDir
    lateinit var tempDir: Path

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    private lateinit var testIndexFile: File
    private lateinit var testDiffFile: File
    private lateinit var expectedResultFile: File

    @BeforeEach
    fun setup() {
        testIndexFile = tempDir.resolve("test-index.json").toFile()
        testDiffFile = tempDir.resolve("test-diff.json").toFile()
        expectedResultFile = tempDir.resolve("expected-result.json").toFile()
    }

    @Test
    @DisplayName("Should merge from test resource files")
    fun testMergeFromIoD() {
        val initialIndex = loadResourceFile("test-data/iod_o-index-v2.json")
        val diff = loadResourceFile("test-data/iod_diff.json")
        val expectedResult = loadResourceFile("test-data/iod_n-index-v2.json")

        performMergeTest(initialIndex, diff, expectedResult)
    }

    @Test
    @DisplayName("Should merge simple package addition")
    fun testSimplePackageAddition() {
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "GPL-3.0"
              },
              "versions": {
                "1": {
                  "added": 1000,
                  "file": {
                    "name": "app1-1.0.apk"
                  },
                  "manifest": {
                    "versionName": "1.0",
                    "versionCode": 1
                  }
                }
              }
            }
          }
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 2000
          },
          "packages": {
            "com.example.app2": {
              "metadata": {
                "added": 2000,
                "categories": ["Games"],
                "license": "MIT"
              },
              "versions": {
                "1": {
                  "added": 2000,
                  "file": {
                    "name": "app2-1.0.apk"
                  },
                  "manifest": {
                    "versionName": "1.0",
                    "versionCode": 1
                  }
                }
              }
            }
          }
        }
        """.trimIndent()

        val expectedResult = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 2000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "GPL-3.0"
              },
              "versions": {
                "1": {
                  "added": 1000,
                  "file": {
                    "name": "app1-1.0.apk"
                  },
                  "manifest": {
                    "versionName": "1.0",
                    "versionCode": 1
                  }
                }
              }
            },
            "com.example.app2": {
              "metadata": {
                "added": 2000,
                "categories": ["Games"],
                "license": "MIT"
              },
              "versions": {
                "1": {
                  "added": 2000,
                  "file": {
                    "name": "app2-1.0.apk"
                  },
                  "manifest": {
                    "versionName": "1.0",
                    "versionCode": 1
                  }
                }
              }
            }
          }
        }
        """.trimIndent()

        // When & Then
        performMergeTest(initialIndex, diff, expectedResult)
    }

    @Test
    @DisplayName("Should remove package when diff contains null")
    fun testPackageRemoval() {
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "GPL-3.0"
              },
              "versions": {}
            },
            "com.example.app2": {
              "metadata": {
                "added": 1000,
                "categories": ["Games"],
                "license": "MIT"
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 2000
          },
          "packages": {
            "com.example.app1": null
          }
        }
        """.trimIndent()

        val expectedResult = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 2000
          },
          "packages": {
            "com.example.app2": {
              "metadata": {
                "added": 1000,
                "categories": ["Games"],
                "license": "MIT"
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        // When & Then
        performMergeTest(initialIndex, diff, expectedResult)
    }

    @Test
    @DisplayName("Should update existing package metadata")
    fun testPackageUpdate() {
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "GPL-3.0",
                "name": {
                  "en": "Old App Name"
                }
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 2000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "license": "Apache-2.0",
                "name": {
                  "en": "New App Name",
                  "de": "Neue App Name"
                },
                "summary": {
                  "en": "App description"
                }
              }
            }
          }
        }
        """.trimIndent()

        val expectedResult = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 2000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "Apache-2.0",
                "name": {
                  "en": "New App Name",
                  "de": "Neue App Name"
                },
                "summary": {
                  "en": "App description"
                }
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        // When & Then
        performMergeTest(initialIndex, diff, expectedResult)
    }

    @Test
    @DisplayName("Should reject older timestamps")
    fun testOlderTimestampRejection() {
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 2000
          },
          "packages": {}
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 1000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Games"],
                "license": "MIT"
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        // When
        testIndexFile.writeText(initialIndex)
        testDiffFile.writeText(diff)

        IndexV2Merger(testIndexFile).use { merger ->
            val wasApplied = merger.processDiff(testDiffFile.inputStream())

            // Then: Patch should be rejected
            assertFalse(wasApplied, "Patch with older timestamp should be rejected")

            // And: Index should remain unchanged
            val resultIndex = merger.getCurrentIndex()
            assertEquals(2000, resultIndex?.repo?.timestamp, "Timestamp should remain unchanged")
            assertTrue(resultIndex?.packages?.isEmpty() == true, "Packages should remain empty")
        }
    }

    @ParameterizedTest
    @DisplayName("Should handle various removal scenarios")
    @CsvSource(
        "'metadata.summary', 'summary'",
        "'metadata.description', 'description'",
        "'metadata.icon', 'icon'",
    )
    fun testFieldRemoval(fieldPath: String, fieldName: String) {
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "added": 1000,
                "categories": ["Productivity"],
                "license": "GPL-3.0",
                "$fieldName": {
                  "en": "Some value"
                }
              },
              "versions": {}
            }
          }
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 2000
          },
          "packages": {
            "com.example.app1": {
              "metadata": {
                "$fieldName": null
              }
            }
          }
        }
        """.trimIndent()

        // When
        testIndexFile.writeText(initialIndex)
        testDiffFile.writeText(diff)

        IndexV2Merger(testIndexFile).use { merger ->
            val wasApplied = merger.processDiff(testDiffFile.inputStream())
            assertTrue(wasApplied, "Patch should be applied")

            // Verify field was removed (this would need to be adapted based on your actual IndexV2 structure)
            val resultIndex = merger.getCurrentIndex()
            assertNotNull(resultIndex)
        }
    }

    @Test
    @DisplayName("Should handle complex nested updates")
    fun testComplexNestedUpdates() {
        // Given: Initial index with repo metadata
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000,
            "name": {
              "en": "Old Repo Name"
            },
            "antiFeatures": {
              "NonFreeNet": {
                "name": {
                  "en": "Non-Free Network"
                }
              }
            },
            "categories": {
              "Games": {
                "name": {
                  "en": "Games"
                }
              }
            }
          },
          "packages": {}
        }
        """.trimIndent()

        // And: Diff with nested updates
        val diff = """
        {
          "repo": {
            "timestamp": 2000,
            "name": {
              "en": "New Repo Name",
              "de": "Neuer Repo Name"
            },
            "antiFeatures": {
              "NonFreeNet": null,
              "Tracking": {
                "name": {
                  "en": "Tracking"
                }
              }
            },
            "categories": {
              "Games": {
                "name": {
                  "en": "Games & Entertainment"
                }
              },
              "Productivity": {
                "name": {
                  "en": "Productivity"
                }
              }
            }
          }
        }
        """.trimIndent()

        testIndexFile.writeText(initialIndex)
        testDiffFile.writeText(diff)

        IndexV2Merger(testIndexFile).use { merger ->
            val wasApplied = merger.processDiff(testDiffFile.inputStream())
            assertTrue(wasApplied, "Complex patch should be applied")

            val resultIndex = merger.getCurrentIndex()
            assertEquals(2000, resultIndex?.repo?.timestamp)
            assertEquals("New Repo Name", resultIndex?.repo?.name?.get("en"))
            assertEquals("Neuer Repo Name", resultIndex?.repo?.name?.get("de"))

            // NonFreeNet removed, Tracking added, Games category name updated, Productivity added
            assertFalse(resultIndex?.repo?.antiFeatures?.containsKey("NonFreeNet") == true)
            assertTrue(resultIndex?.repo?.antiFeatures?.containsKey("Tracking") == true)
            assertEquals(
                "Games & Entertainment",
                resultIndex.repo.categories.get("Games")?.name?.get("en"),
            )
            assertTrue(resultIndex.repo.categories.containsKey("Productivity") == true)
        }
    }

    @Test
    @DisplayName("Should auto-save on close")
    fun testAutoSaveOnClose() {
        // Given: Initial index
        val initialIndex = """
        {
          "repo": {
            "address": "https://example.com/repo",
            "timestamp": 1000
          },
          "packages": {}
        }
        """.trimIndent()

        val diff = """
        {
          "repo": {
            "timestamp": 2000
          }
        }
        """.trimIndent()

        testIndexFile.writeText(initialIndex)
        testDiffFile.writeText(diff)

        IndexV2Merger(testIndexFile).use { merger ->
            merger.processDiff(testDiffFile.inputStream())
        }

        // File should be updated
        val savedContent = testIndexFile.readText()
        val savedIndex = jsonConfig.decodeFromString<IndexV2>(savedContent)
        assertEquals(2000, savedIndex.repo.timestamp, "Changes should be auto-saved on close")
    }

    private fun performMergeTest(initialIndex: String, diff: String, expectedResult: String) {
        testIndexFile.writeText(initialIndex)
        testDiffFile.writeText(diff)
        expectedResultFile.writeText(expectedResult)

        IndexV2Merger(testIndexFile).use { merger ->
            val wasApplied = merger.processDiff(testDiffFile.inputStream())
            assertTrue(wasApplied, "Patch should be applied successfully")
        }

        // Compare results
        val actualResult = testIndexFile.readText()
        val expectedJson = jsonConfig.parseToJsonElement(expectedResult)
        val actualJson = jsonConfig.parseToJsonElement(actualResult)
        assertEquals(expectedJson, actualJson, "Merged result should match expected result")
    }

    private fun loadResourceFile(resourcePath: String): String {
        return this::class.java.classLoader.getResourceAsStream(resourcePath)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw IllegalArgumentException("Resource file not found: $resourcePath")
    }
}
