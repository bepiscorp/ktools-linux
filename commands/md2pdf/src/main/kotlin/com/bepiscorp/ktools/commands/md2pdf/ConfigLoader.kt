package com.bepiscorp.ktools.commands.md2pdf

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

/** Configuration data for md2pdf. */
data class Md2PdfConfig(
    val configFile: String? = null,
    val theme: String? = null,
    val pageSize: String? = null,
    val margin: String? = null,
    val css: String? = null,
    val template: String? = null,
    val toc: Boolean? = null,
    val timeout: Int? = null,
    val parallel: Int? = null,
    val engine: String? = null,
    val allowRemote: Boolean? = null
)

/** Loads and parses md2pdf configuration files. */
class ConfigLoader {

    /** Load configuration from file or return default. */
    fun load(configFile: File?): Md2PdfConfig {
        if (configFile == null || !configFile.exists()) {
            return Md2PdfConfig()
        }

        return try {
            val config = ConfigFactory.parseFile(configFile)
            parseConfig(config, configFile.path)
        } catch (e: Exception) {
            // Log warning but continue with defaults
            Md2PdfConfig(configFile = configFile.path)
        }
    }

    private fun parseConfig(config: Config, configPath: String): Md2PdfConfig {
        return Md2PdfConfig(
            configFile = configPath,
            theme = config.getOptionalString("md2pdf.theme"),
            pageSize = config.getOptionalString("md2pdf.page-size"),
            margin = config.getOptionalString("md2pdf.margin"),
            css = config.getOptionalString("md2pdf.css"),
            template = config.getOptionalString("md2pdf.template"),
            toc = config.getOptionalBoolean("md2pdf.toc"),
            timeout = config.getOptionalInt("md2pdf.timeout"),
            parallel = config.getOptionalInt("md2pdf.parallel"),
            engine = config.getOptionalString("md2pdf.engine"),
            allowRemote = config.getOptionalBoolean("md2pdf.allow-remote")
        )
    }

    /** Generate a sample configuration file. */
    fun generateSampleConfig(): String {
        return """
# ktools md2pdf configuration file
# All settings are optional and can be overridden by CLI flags

md2pdf {
  # Engine selection: auto, native, docker
  engine = "auto"
  
  # Default theme: github, classic, or path to custom theme
  theme = "github"
  
  # Page size: A4, Letter, Legal, etc.
  page-size = "A4"
  
  # Margins: top,right,bottom,left with CSS units
  margin = "20mm,15mm,20mm,15mm"
  
  # Custom CSS file path
  # css = "custom.css"
  
  # Custom HTML template path
  # template = "template.html"
  
  # Generate table of contents by default
  toc = false
  
  # Conversion timeout in seconds
  timeout = 60
  
  # Parallel processing limit
  parallel = 4
  
  # Allow remote URL inputs
  allow-remote = false
}

# Theme-specific configurations
themes {
  github {
    css = "themes/github.css"
    font-family = "system-ui, -apple-system, sans-serif"
  }
  
  classic {
    css = "themes/classic.css"
    font-family = "serif"
  }
}
        """.trimIndent()
    }

    private fun Config.getOptionalString(path: String): String? {
        return if (hasPath(path)) getString(path) else null
    }

    private fun Config.getOptionalBoolean(path: String): Boolean? {
        return if (hasPath(path)) getBoolean(path) else null
    }

    private fun Config.getOptionalInt(path: String): Int? {
        return if (hasPath(path)) getInt(path) else null
    }
}
