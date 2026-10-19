//import android.content.Context
//import android.graphics.Bitmap
//import org.apache.poi.xwpf.usermodel.*
//import android.graphics.BitmapFactory
//import android.graphics.Color
//import android.graphics.Typeface
//import android.text.Layout
//import android.text.Spannable
//import android.text.SpannableStringBuilder
//import android.text.style.*
//
//import java.io.File
//import java.io.FileInputStream
//import android.graphics.pdf.PdfDocument
//
//sealed class DocElement {
//    data class TextParagraph(
//        val text: SpannableStringBuilder,
//        val alignment: Int,
//        val fontSize: Int,
//        val lineSpacing: Float = 1.0f,
//        val bulletStyle: BulletStyle? = null
//    ) : DocElement()
//
//    data class ImageElement(
//        val bitmap: Bitmap,
//        val width: Int,
//        val height: Int,
//        val alignment: Int
//    ) : DocElement()
//
//    data class TableElement(
//        val rows: List<List<SpannableStringBuilder>>,
//        val borderWidth: Int,
//        val cellPadding: Int
//    ) : DocElement()
//
//    data class BulletStyle(
//        val level: Int,
//        val numberingFormat: String? = null // null for bullets, string format for numbered lists
//    )
//}
//
//class DocxReader(private val context: Context) {
//    public fun readCompleteDocx(fileName: String): List<DocElement> {
//        return try {
//            val filesDir = context.getExternalFilesDir("DownloadedFiles")
//            val file = File(filesDir, fileName)
//
//            if (!file.exists()) {
//                return listOf(DocElement.TextParagraph(
//                    SpannableStringBuilder("File not found: ${file.absolutePath}"),
//                    Layout.Alignment.ALIGN_NORMAL.ordinal,
//                    14
//                ))
//            }
//
//            val inputStream = FileInputStream(file)
//            val document = XWPFDocument(inputStream)
//            val elements = mutableListOf<DocElement>()
//            val pdf = PdfDocument()
//
//            // Process document body
//            document.bodyElements.forEach { bodyElement ->
//                when (bodyElement) {
//                    is XWPFParagraph -> processParagraph(bodyElement)?.let { elements.add(it) }
//                    is XWPFTable -> processTable(bodyElement)?.let { elements.add(it) }
//                }
//            }
//
//            document.close()
//            inputStream.close()
//            elements
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            listOf(DocElement.TextParagraph(
//                SpannableStringBuilder("Error reading file: ${e.message}"),
//                Layout.Alignment.ALIGN_NORMAL.ordinal,
//                14
//            ))
//        }
//    }
//
//    private fun processParagraph(paragraph: XWPFParagraph): DocElement? {
//        // Handle images
//        val pictures = paragraph.runs
//            .flatMap { it.embeddedPictures }
//
//        if (pictures.isNotEmpty()) {
//            return processImage(pictures.first(), paragraph.alignment)
//        }
//
//        // Handle text paragraph
//        val spannableText = SpannableStringBuilder()
//        var currentPosition = 0
//
//        for (run in paragraph.runs) {
//            val text = run.text()
//            if (text.isEmpty()) continue
//
//            spannableText.append(text)
//            applyTextStyles(spannableText, run, currentPosition, text.length)
//            currentPosition += text.length
//        }
//
//        // Get bullet or numbering information
//        val bulletStyle = paragraph.numFmt?.let { format ->
//            DocElement.BulletStyle(
//                level = paragraph.numIlvl?.toInt() ?: 0,
//                numberingFormat = if (format == "bullet") null else format
//            )
//        }
//
//        // Get line spacing from paragraph properties
//        val lineSpacing = try {
//            val spacingBetween = paragraph.getCTP().pPr.spacing?.lineRule?.toString()
//            when (spacingBetween) {
//                "AUTO" -> 1.0f
//                "EXACT" -> 1.0f
//                "AT_LEAST" -> 1.15f
//                else -> 1.0f
//            }
//        } catch (e: Exception) {
//            1.0f
//        }
//
//        return DocElement.TextParagraph(
//            text = spannableText,
//            alignment = convertAlignment(paragraph.alignment),
//            fontSize = paragraph.runs.firstOrNull()?.fontSize ?: 14,
//            lineSpacing = lineSpacing,
//            bulletStyle = bulletStyle
//        )
//    }
//
//    private fun processImage(picture: XWPFPicture, alignment: ParagraphAlignment): DocElement.ImageElement? {
//        return try {
//            val pictureData = picture.pictureData
//            val imageBytes = pictureData.data
//
//            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//
//            // Get image dimensions in EMUs (English Metric Units)
//            val ctPicture = picture.getCTPicture()
//            val widthEmu = ctPicture.spPr.xfrm.ext.cx
//            val heightEmu = ctPicture.spPr.xfrm.ext.cy
//
//            // Convert EMUs to pixels (1 EMU = 1/914400 inch, assuming 96 DPI)
//            val widthInPixels = (widthEmu * 96 / 914400).toInt()
//            val heightInPixels = (heightEmu * 96 / 914400).toInt()
//
//            DocElement.ImageElement(
//                bitmap = bitmap,
//                width = widthInPixels,
//                height = heightInPixels,
//                alignment = convertAlignment(alignment)
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    private fun processTable(table: XWPFTable): DocElement.TableElement {
//        val tableRows = table.rows.map { row ->
//            row.tableCells.map { cell ->
//                val spannableText = SpannableStringBuilder()
//
//                cell.paragraphs.forEach { paragraph ->
//                    paragraph.runs.forEach { run ->
//                        val startPosition = spannableText.length
//                        spannableText.append(run.text())
//                        applyTextStyles(spannableText, run, startPosition, run.text().length)
//                    }
//                    spannableText.append("\n")
//                }
//
//                spannableText
//            }
//        }
//
//        // Get border width from table properties
//        val borderWidth = table.ctTbl?.tblPr?.tblBorders?.top?.sz?.toInt() ?: 1
//
//        return DocElement.TableElement(
//            rows = tableRows,
//            borderWidth = borderWidth,
//            cellPadding = 8
//        )
//    }
//    private fun applyTextStyles(spannable: SpannableStringBuilder, run: XWPFRun, start: Int, length: Int) {
//        // Basic formatting
//        if (run.isBold) {
//            spannable.setSpan(StyleSpan(Typeface.BOLD), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//        if (run.isItalic) {
//            spannable.setSpan(StyleSpan(Typeface.ITALIC), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//        if (run.underline != UnderlinePatterns.NONE) {
//            spannable.setSpan(UnderlineSpan(), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//
//        // Text color
//        run.color?.let { color ->
//            try {
//                val colorInt = Color.parseColor("#$color")
//                spannable.setSpan(ForegroundColorSpan(colorInt), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            } catch (e: IllegalArgumentException) {}
//        }
//
//        // Background highlight
//        run.textHighlightColor?.let { highlight ->
//            try {
//                val highlightColor = when (highlight) {
//                    STHighlightColor.YELLOW -> Color.YELLOW
//                    STHighlightColor.GREEN -> Color.GREEN
//                    STHighlightColor.CYAN -> Color.CYAN
//                    STHighlightColor.MAGENTA -> Color.MAGENTA
//                    STHighlightColor.BLUE -> Color.BLUE
//                    STHighlightColor.RED -> Color.RED
//                    else -> null
//                }
//                highlightColor?.let {
//                    spannable.setSpan(BackgroundColorSpan(it), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                }
//            } catch (e: IllegalArgumentException) {}
//        }
//
//        // Handle vertical alignment (subscript/superscript)
//        try {
//            when (run.getCTR().rPr.getVertAlignArray()?.firstOrNull()?.getVal().toString()) {
//                "subscript" -> spannable.setSpan(SubscriptSpan(), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                "superscript" -> spannable.setSpan(SuperscriptSpan(), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            }
//        } catch (e: Exception) {
//            // Handle potential null pointer exceptions
//        }
//
//        // Strike-through
//        if (run.isStrikeThrough) {
//            spannable.setSpan(StrikethroughSpan(), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//    }
//
//
//    private fun convertAlignment(alignment: ParagraphAlignment): Int {
//        return when (alignment) {
//            ParagraphAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER.ordinal
//            ParagraphAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE.ordinal
//            else -> Layout.Alignment.ALIGN_NORMAL.ordinal
//        }
//    }
//}