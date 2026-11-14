package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Css
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.ProjectTemplateId
import com.example.ui.theme.CssBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.HtmlOrange
import com.example.ui.theme.JsYellow
import com.example.ui.theme.JsonGreen
import com.example.ui.theme.MdPurple
import com.example.ui.theme.SvgPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileVisualSpec(
    val icon: ImageVector,
    val color: Color,
    val badge: String
)

fun fileVisualFor(fileName: String, isDirectory: Boolean, isExpanded: Boolean = false): FileVisualSpec {
    if (isDirectory) {
        return FileVisualSpec(
            icon = if (isExpanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
            color = ElectricCyan,
            badge = "DIR"
        )
    }
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (ext) {
        "html", "htm" -> FileVisualSpec(Icons.Rounded.Html, HtmlOrange, "HTML")
        "css" -> FileVisualSpec(Icons.Rounded.Css, CssBlue, "CSS")
        "js", "mjs" -> FileVisualSpec(Icons.Rounded.Javascript, JsYellow, "JS")
        "json" -> FileVisualSpec(Icons.Rounded.DataObject, JsonGreen, "JSON")
        "svg" -> FileVisualSpec(Icons.Rounded.Image, SvgPink, "SVG")
        "xml" -> FileVisualSpec(Icons.Rounded.Code, SvgPink, "XML")
        "md" -> FileVisualSpec(Icons.Rounded.Description, MdPurple, "MD")
        else -> FileVisualSpec(Icons.Rounded.Description, EmeraldMint, ext.uppercase().ifEmpty { "TXT" })
    }
}

fun templateIconFor(templateId: ProjectTemplateId): ImageVector = when (templateId) {
    ProjectTemplateId.EMPTY -> Icons.Rounded.FolderOpen
    ProjectTemplateId.BASIC_HTML -> Icons.Rounded.Web
    ProjectTemplateId.WEB_BUNDLE -> Icons.Rounded.Layers
    ProjectTemplateId.RESPONSIVE -> Icons.Rounded.Language
    ProjectTemplateId.CUSTOM -> Icons.Rounded.Tune
}

fun projectAccentColor(index: Int): Color = when (index % 5) {
    0 -> ElectricCyan
    1 -> MdPurple
    2 -> HtmlOrange
    3 -> EmeraldMint
    else -> SvgPink
}

fun formatRelativeDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}
