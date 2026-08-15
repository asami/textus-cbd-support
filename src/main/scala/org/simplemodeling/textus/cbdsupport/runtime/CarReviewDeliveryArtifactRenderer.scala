package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

/*
 * @since   Jul. 23, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Deterministic, in-memory delivery artifacts for one already-projected Review
 * document. This renderer has no provider, repository, clock, filesystem, or
 * external PDF-converter dependency.
 */
final case class CarReviewDeliveryArtifacts(
  markdown: String,
  pdf: Array[Byte],
  limitations: Vector[String]
)

object CarReviewDeliveryArtifactRenderer {
  private final case class Line(role: String, text: String)
  private final case class PdfPage(lines: Vector[Line])
  private final case class PdfTable(objectid: Int, rowobjectids: Vector[Int], lineindexes: Vector[Int])

  private val _pdf_page_width = 612
  private val _pdf_page_height = 792
  private val _pdf_top = 756
  private val _pdf_bottom = 42
  private val _pdf_line_height = 13
  private val _pdf_line_width = 72

  def render(document: CarReviewDeliveryDocument): CarReviewDeliveryArtifacts = {
    val lines = _lines(document)
    val limitations = _pdf_limitations(lines)
    CarReviewDeliveryArtifacts(
      _markdown(lines, limitations),
      _pdf(lines, limitations),
      limitations
    )
  }

  private def _lines(document: CarReviewDeliveryDocument): Vector[Line] = {
    val dashboard = document.dashboard
    val identity = Vector(
      Line("H1", "CBD CAR Review"),
      Line("H2", "Report identity"),
      Line("TH", "Field | Value"),
      Line("TD", s"Review | ${dashboard.reviewId.value}"),
      Line("TD", s"Report | ${dashboard.reportId.value}"),
      Line("TD", s"Report digest | ${dashboard.reportDigest.value}"),
      Line("TD", s"Target | ${dashboard.target.kind.value}:${dashboard.target.name}@${dashboard.target.digest.value}"),
      Line("TD", s"Profile | ${dashboard.profile.value}")
    )
    val gate = Vector(
      Line("H2", "Gate"),
      Line("TH", "Field | Value"),
      Line("TD", s"Result | ${dashboard.gate.result.value}"),
      Line("TD", s"Policy | ${dashboard.gate.policyId}@${dashboard.gate.policyVersion.value}")
    ) ++ dashboard.gate.reasons.map(reason => Line("P", s"Reason: $reason")) ++
      dashboard.gate.blockingObservationIds.map(id => Line("P", s"Blocking observation: ${id.value}"))
    val counts = Vector(
      Line("H2", "Dashboard"),
      Line("TH", "Finding | Assurance | Unknown"),
      Line("TD", s"${dashboard.findingCount} | ${dashboard.assuranceCount} | ${dashboard.unknownCount}")
    )
    val baseline = dashboard.baseline.toVector.flatMap { value =>
      Vector(
        Line("H2", "Baseline"),
        Line("P", s"Report: ${value.reportId.value}"),
        Line("P", s"Report digest: ${value.reportDigest.value}"),
        Line("P", s"Added observations: ${_ids(value.addedObservationIds.map(_.value))}"),
        Line("P", s"Removed observations: ${_ids(value.removedObservationIds.map(_.value))}"),
        Line("P", s"Unchanged observations: ${_ids(value.unchangedObservationIds.map(_.value))}")
      )
    }
    val capabilities = Vector(Line("H2", "Capabilities")) ++ document.capabilities.flatMap { value =>
      Vector(
        Line("H3", value.id.value),
        Line("P", s"Applicability: ${value.applicability.value}; maturity: ${value.maturity.value}; confidence: ${value.confidence.value}"),
        Line("P", s"Observations: ${_ids(value.observationIds.map(_.value))}"),
        Line("P", s"Evidence: ${_ids(value.evidenceIds.map(_.value))}"),
        Line("P", s"Providers: ${_ids(value.providerIds.map(_.value))}")
      ) ++ value.coverage.toVector.map { coverage =>
        Line("P", s"Coverage: ${coverage.assessedSubjects}/${coverage.applicableSubjects}; Unknown: ${coverage.unknownSubjects}")
      } ++ value.strengths.map(text => Line("P", s"Strength: $text")) ++ value.gaps.map(text => Line("P", s"Gap: $text"))
    }
    val qualitycoverage = Vector(
      Line("H2", "Quality Coverage"),
      Line("TH", "Check | Capability | State")
    ) ++ document.qualityCoverage.flatMap { value =>
      Vector(
        Line("TD", s"${value.checkId.value} | ${value.capabilityId.value} | ${value.state.value}"),
        Line("P", s"Observations: ${_ids(value.observationIds.map(_.value))}"),
        Line("P", s"Evidence: ${_ids(value.evidenceIds.map(_.value))}")
      ) ++ value.limitation.toVector.map { limitation =>
        Line("P", s"Limitation: ${limitation.scope.value}:${limitation.code} [${limitation.subjectId.getOrElse("report")}] ${limitation.message}")
      }
    }
    val observations = Vector(Line("H2", "Observations")) ++ document.observations.flatMap { value =>
      Vector(
        Line("H3", s"${value.id.value} [${value.`type`.value}]"),
        Line("P", s"Rule: ${value.rule.id.value}@${value.rule.version.value}"),
        Line("P", s"Disposition: ${value.disposition.state.value}"),
        Line("P", s"Evidence: ${_ids(value.evidenceIds.map(_.value))}"),
        Line("P", s"Capabilities: ${_ids(value.capabilityIds.map(_.value))}"),
        Line("P", s"Provider: ${value.provider.provider.id.value}@${value.provider.provider.version.value}"),
        Line("P", s"Locations: ${_texts(value.locations)}"),
        Line("P", s"Message: ${value.message}")
      )
    }
    val limitations = Vector(Line("H2", "Limitations")) ++ document.limitations.map { value =>
      Line("P", s"${value.scope.value}:${value.code} [${value.subjectId.getOrElse("report")}] ${value.message}")
    }
    identity ++ gate ++ counts ++ baseline ++ capabilities ++ qualitycoverage ++ observations ++ limitations ++
      Vector(Line("H2", "Redaction and omissions"), Line("P", "Delivery-safe text only; canonical Evidence facts, rationale, raw provider data, credentials, and unsafe locations are omitted upstream."))
  }

  private def _markdown(lines: Vector[Line], limitations: Vector[String]): String = {
    val body = lines.map {
      case Line("H1", text) => s"# ${_markdown_text(text)}"
      case Line("H2", text) => s"## ${_markdown_text(text)}"
      case Line("H3", text) => s"### ${_markdown_text(text)}"
      case Line("TH", text) => s"| ${_markdown_columns(text).mkString(" | ")} |\n|---|---|"
      case Line("TD", text) => s"| ${_markdown_columns(text).mkString(" | ")} |"
      case Line(_, text) => s"- ${_markdown_text(text)}"
    }
    val renderer = limitations.map(value => s"- PDF renderer limitation: $value")
    (body ++ renderer).mkString("\n") + "\n"
  }

  private def _pdf(lines: Vector[Line], limitations: Vector[String]): Array[Byte] = {
    val artifactlines = lines ++ limitations.map(value => Line("P", s"PDF renderer limitation: $value"))
    val pages = _pages(artifactlines.flatMap(_wrap_pdf_line))
    val pagebase = 7
    val pageobjects = pages.indices.map(index => pagebase + (index * 2)).toVector
    val contentobjects = pageobjects.map(_ + 1)
    val structurebase = pagebase + (pages.size * 2)
    val pageoffsets = pages.scanLeft(0)(_ + _.lines.size)
    val structured = pages.zipWithIndex.flatMap { case (page, pageindex) =>
      page.lines.zipWithIndex.map { case (line, mcid) =>
        (line, structurebase + pageoffsets(pageindex) + mcid, mcid, pageobjects(pageindex))
      }
    }
    val parenttreeobject = 6
    val structrootobject = 5
    val tables = _pdf_tables(structured, structurebase + structured.size)
    val tablelineindexes = tables.flatMap(_.lineindexes).toSet
    val tablebyline = tables.flatMap(table => table.lineindexes.zip(table.rowobjectids)).toMap
    val rootchildren = structured.indices.flatMap { index =>
      tables.find(_.lineindexes.headOption.contains(index)).map(_.objectid).orElse {
        Option.when(!tablelineindexes.contains(index))(structured(index)._2)
      }
    }
    val objects = scala.collection.mutable.Map.empty[Int, String]
    objects += 1 -> s"<< /Type /Catalog /Pages 2 0 R /MarkInfo << /Marked true >> /StructTreeRoot $structrootobject 0 R /Lang (en-US) >>"
    objects += 2 -> s"<< /Type /Pages /Kids [${pageobjects.map(x => s"$x 0 R").mkString(" ")}] /Count ${pages.size} >>"
    objects += 3 -> "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
    objects += 4 -> "<< /Title (CBD CAR Review) /Producer (textus-cbd-support deterministic renderer) >>"
    objects += structrootobject -> s"<< /Type /StructTreeRoot /K [${rootchildren.map(x => s"$x 0 R").mkString(" ")}] /ParentTree $parenttreeobject 0 R >>"
    val parententries = pages.indices.map { pageindex =>
      val refs = pages(pageindex).lines.indices.map(index => structured(pageoffsets(pageindex) + index)._2).map(x => s"$x 0 R").mkString(" ")
      s"$pageindex [$refs]"
    }
    objects += parenttreeobject -> s"<< /Nums [${parententries.mkString(" ")}] >>"
    pages.indices.foreach { pageindex =>
      val pageobject = pageobjects(pageindex)
      val contentobject = contentobjects(pageindex)
      objects += pageobject -> s"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $_pdf_page_width $_pdf_page_height] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentobject 0 R /StructParents $pageindex >>"
      val pagelines = pages(pageindex).lines
      val content = _pdf_content(pagelines)
      objects += contentobject -> s"<< /Length ${content.getBytes(StandardCharsets.ISO_8859_1).length} >>\nstream\n$content\nendstream"
    }
    tables.foreach { table =>
      objects += table.objectid -> s"<< /Type /StructElem /S /Table /P $structrootobject 0 R /K [${table.rowobjectids.map(x => s"$x 0 R").mkString(" ")}] >>"
      table.rowobjectids.zip(table.lineindexes).foreach { case (rowobject, lineindex) =>
        objects += rowobject -> s"<< /Type /StructElem /S /TR /P ${table.objectid} 0 R /K ${structured(lineindex)._2} 0 R >>"
      }
    }
    structured.zipWithIndex.foreach { case ((line, objectid, mcid, pageobject), lineindex) =>
      val parent = tablebyline.getOrElse(lineindex, structrootobject)
      objects += objectid -> s"<< /Type /StructElem /S /${line.role} /P $parent 0 R /Pg $pageobject 0 R /K $mcid >>"
    }
    _pdf_document(objects.toMap, 1, 4)
  }

  private def _pdf_content(lines: Vector[Line]): String =
    lines.zipWithIndex.map { case (line, index) =>
      val y = _pdf_top - (index * _pdf_line_height)
      val size = line.role match {
        case "H1" => 16
        case "H2" => 13
        case "H3" => 11
        case _ => 9
      }
      s"/${line.role} <</MCID $index>> BDC BT /F1 $size Tf 36 $y Td (${_pdf_text(line.text)}) Tj ET EMC"
    }.mkString("\n")

  private def _pdf_document(objects: Map[Int, String], root: Int, info: Int): Array[Byte] = {
    val header = "%PDF-1.7\n%----\n"
    val ordered = objects.toVector.sortBy(_._1)
    val builder = new StringBuilder(header)
    val offsets = ordered.map { case (id, value) =>
      val offset = builder.toString.getBytes(StandardCharsets.ISO_8859_1).length
      builder.append(s"$id 0 obj\n$value\nendobj\n")
      id -> offset
    }
    val startxref = builder.toString.getBytes(StandardCharsets.ISO_8859_1).length
    builder.append(s"xref\n0 ${ordered.last._1 + 1}\n0000000000 65535 f \n")
    offsets.foreach { case (_, offset) => builder.append(f"$offset%010d 00000 n \n") }
    builder.append(s"trailer\n<< /Size ${ordered.last._1 + 1} /Root $root 0 R /Info $info 0 R >>\nstartxref\n$startxref\n%%EOF\n")
    builder.toString.getBytes(StandardCharsets.ISO_8859_1)
  }

  private def _pages(lines: Vector[Line]): Vector[PdfPage] = {
    val capacity = ((_pdf_top - _pdf_bottom) / _pdf_line_height) + 1
    val (completed, current) = lines.foldLeft((Vector.empty[PdfPage], Vector.empty[Line])) { case ((pages, page), line) =>
      val minimumfollowing = line.role match {
        case "H1" => 8
        case "H2" => 6
        case "H3" => 10
        case _ => 1
      }
      if (page.nonEmpty && (page.size >= capacity || page.size + minimumfollowing > capacity)) {
        (pages :+ PdfPage(page), Vector(line))
      } else {
        (pages, page :+ line)
      }
    }
    completed :+ PdfPage(current)
  }

  private def _wrap_pdf_line(line: Line): Vector[Line] = {
    val text = _pdf_safe_text(line.text)
    _wrap_pdf_text(text).zipWithIndex.map { case (part, index) =>
      Line(if (index == 0 || line.role == "TH" || line.role == "TD") line.role else "P", part)
    }.toVector
  }

  private def _pdf_tables(
    structured: Vector[(Line, Int, Int, Int)],
    firstobjectid: Int
  ): Vector[PdfTable] = {
    val groups = scala.collection.mutable.ArrayBuffer.empty[Vector[Int]]
    var index = 0
    while (index < structured.size) {
      if (structured(index)._1.role == "TH") {
        val start = index
        index += 1
        while (index < structured.size && structured(index)._1.role == "TD")
          index += 1
        groups += (start until index).toVector
      } else {
        index += 1
      }
    }
    val (_, tables) = groups.foldLeft((firstobjectid, Vector.empty[PdfTable])) { case ((nextobject, acc), lines) =>
      val table = PdfTable(nextobject, lines.indices.map(offset => nextobject + offset + 1).toVector, lines)
      (nextobject + lines.size + 1, acc :+ table)
    }
    tables
  }

  private def _wrap_pdf_text(value: String): Vector[String] = {
    val words = value.split(" ", -1).toVector
    val (lines, current) = words.foldLeft((Vector.empty[String], "")) { case ((acc, line), word) =>
      if (word.length > _pdf_line_width) {
        val flushed = if (line.nonEmpty) acc :+ line else acc
        val parts = word.grouped(_pdf_line_width).toVector
        (flushed ++ parts.dropRight(1), parts.lastOption.getOrElse(""))
      } else if (line.isEmpty) {
        (acc, word)
      } else if ((line.length + 1 + word.length) <= _pdf_line_width) {
        (acc, s"$line $word")
      } else {
        (acc :+ line, word)
      }
    }
    (lines :+ current).filter(_.nonEmpty)
  }

  private def _pdf_limitations(lines: Vector[Line]): Vector[String] =
    Vector("pdf.unsupported-character").filter(_ => lines.exists(line => line.text.exists(ch => ch < ' ' || ch > '~')))

  private def _pdf_safe_text(value: String): String =
    value.flatMap {
      case ch if ch >= ' ' && ch <= '~' => ch.toString
      case ch => f"[omitted: U+${ch.toInt}%04X]"
    }

  private def _pdf_text(value: String): String =
    value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

  private def _markdown_text(value: String): String =
    value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", " ")

  private def _markdown_columns(value: String): Vector[String] =
    value.split(" \\| ", 2).toVector.map(_markdown_text)

  private def _ids(values: Vector[String]): String =
    _texts(values)

  private def _texts(values: Vector[String]): String =
    if (values.isEmpty) "none" else values.mkString(", ")
}
