package org.simplemodeling.textus.cbdsupport.runtime

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, RandomAccessFile}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, Paths}
import java.util.Properties
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.jdk.CollectionConverters.*

/**
 * Actual output locators and bounded, recursive exclusion evidence, without extraction.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object InternalModelOutputExclusionFixture {
  final case class Evidence(projectroot: Path, mainjar: Path, apijar: Path,
    generatedroot: Path, metadataroot: Path, scaladocroot: Path, car: Path,
    sar: Path, publicationroot: Path)
  final case class Entry(path: String, tokens: Set[String])
  final case class Leak(path: String, token: String)
  final case class Scan(entries: Vector[Entry], leaks: Vector[Leak]) {
    def containsToken(token: String): Boolean = entries.exists(_.tokens.contains(token))
  }
  final case class ScanLimits(depth: Int = 8, entrybytes: Long = 256L * 1024 * 1024,
    totalbytes: Long = 2L * 1024 * 1024 * 1024)
  private final case class ArchiveMember(name: String, size: Long)
  private val _resource = "internal-model-output-exclusion.properties"
  private val _schema = "cbdsupport.output-exclusion.v1"
  private val _fields = Set("schema", "projectRoot", "mainJar", "apiJar", "generatedRoot",
    "metadataRoot", "scaladocRoot", "car", "sar", "publicationRoot")
  private val _private_tokens = Vector("P106_PRIVATE_MODEL_SENTINEL", "InternalPrivateApi",
    "PrivateRequest", "PrivateResponse", "src/main/internal-model/")
  private val _public_tokens = Vector("P106_PUBLIC_OUTPUT_CONTROL", "P106_PUBLIC_RESOURCE_CONTROL",
    "PublicOutputApi", "PublicRequest", "PublicResponse", "echo", "value")
  private val _tokens = _private_tokens ++ _public_tokens

  def load(): Evidence = {
    val stream = Option(getClass.getClassLoader.getResourceAsStream(_resource))
      .getOrElse(throw new IllegalArgumentException(s"Missing exact Test resource: ${_resource}"))
    readReceipt(stream)
  }

  def readReceipt(inputStream: InputStream): Evidence = {
    val properties = new Properties {
      override def put(key: Object, value: Object): Object = {
        require(!containsKey(key), s"Duplicate receipt field: $key")
        super.put(key, value)
      }
    }
    try properties.load(inputStream) finally inputStream.close()
    decode(properties.stringPropertyNames.asScala.map(key => key -> properties.getProperty(key)).toMap)
  }

  def decode(fields: Map[String, String]): Evidence = {
    require(fields.keySet == _fields, "Missing or extra output receipt fields")
    require(fields("schema") == _schema, "Unsupported output receipt schema")
    def _path_(key: String): Path = {
      val path = Paths.get(fields(key))
      require(path.isAbsolute && path.normalize.toString == fields(key), s"Noncanonical locator: $key")
      _require_path(path)
      path
    }
    val projectroot = _path_("projectRoot")
    require(Files.isDirectory(projectroot), "Missing fixture project root")
    require(projectroot.endsWith(Paths.get("src/test/fixtures/internal-model-output-exclusion")),
      "Receipt must locate the selected consuming fixture")
    val targetroot = projectroot.resolve("target")
    val paths = (_fields - "schema" - "projectRoot").map { key =>
      val path = _path_(key)
      require(path.startsWith(targetroot), s"Output outside selected fixture target: $key")
      if (Set("mainJar", "apiJar", "car", "sar").contains(key))
        require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), s"Missing owner file: $key")
      else require(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS), s"Missing owner directory: $key")
      key -> path
    }.toMap
    Evidence(projectroot, paths("mainJar"), paths("apiJar"), paths("generatedRoot"),
      paths("metadataRoot"), paths("scaladocRoot"), paths("car"), paths("sar"), paths("publicationRoot"))
  }

  def fields(evidence: Evidence): Map[String, String] = Map("schema" -> _schema,
    "projectRoot" -> evidence.projectroot.toString, "mainJar" -> evidence.mainjar.toString,
    "apiJar" -> evidence.apijar.toString, "generatedRoot" -> evidence.generatedroot.toString,
    "metadataRoot" -> evidence.metadataroot.toString, "scaladocRoot" -> evidence.scaladocroot.toString,
    "car" -> evidence.car.toString, "sar" -> evidence.sar.toString,
    "publicationRoot" -> evidence.publicationroot.toString)

  def scan(output: Path, limits: ScanLimits = ScanLimits()): Scan = {
    require(limits.depth >= 0 && limits.entrybytes > 0 && limits.totalbytes > 0, "Invalid scan limits")
    _require_path(output)
    val scanner = new OutputScanner(limits)
    if (Files.isDirectory(output, LinkOption.NOFOLLOW_LINKS)) {
      val walk = Files.walk(output)
      try walk.iterator.asScala.foreach { path =>
        _require_path(path)
        val relative = output.relativize(path).toString.replace('\\', '/')
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) scanner.name(relative)
        else {
          require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), s"Unreadable output entry: $path")
          scanner.file(path, relative)
        }
      } finally walk.close()
    } else scanner.file(output, output.getFileName.toString)
    scanner.result
  }

  def text(path: Path): String = {
    _require_path(path)
    require(Files.size(path) <= 256L * 1024 * 1024, s"Oversized document: $path")
    Files.readString(path, StandardCharsets.UTF_8)
  }

  def zip(name: String, payload: Array[Byte]): Array[Byte] = {
    val buffer = new ByteArrayOutputStream
    val stream = new ZipOutputStream(buffer)
    try {
      stream.putNextEntry(new ZipEntry(name))
      stream.write(payload)
      stream.closeEntry()
    } finally stream.close()
    buffer.toByteArray
  }

  def nested(depth: Int, payload: Array[Byte]): Array[Byte] = {
    require(depth >= 0 && depth <= 8, "Safe test nesting only")
    (0 until depth).foldLeft(zip("payload.txt", payload))((bytes, level) => zip(s"level-$level.jar", bytes))
  }

  def withArchive[A](bytes: Array[Byte])(action: Path => A): A = {
    val workroot = Paths.get("target/internal-model-output-exclusion/work").toAbsolutePath.normalize
    Files.createDirectories(workroot)
    val path = Files.createTempFile(workroot, "p106-detector-", ".zip")
    try {
      Files.write(path, bytes)
      action(path)
    } finally Files.deleteIfExists(path)
  }

  private def _require_path(path: Path): Unit = {
    require(Files.exists(path, LinkOption.NOFOLLOW_LINKS), s"Missing output or input: $path")
    var ancestor = path.toAbsolutePath
    while (ancestor != null) {
      require(!Files.isSymbolicLink(ancestor), s"Symlink is not exclusion evidence: $ancestor")
      ancestor = ancestor.getParent
    }
    require(Files.isReadable(path), s"Unreadable output or input: $path")
  }

  private def _archive(name: String): Boolean =
    Vector(".jar", ".zip", ".car", ".sar").exists(name.toLowerCase(java.util.Locale.ROOT).endsWith)

  private final class OutputScanner(limits: ScanLimits) {
    private var _examined = 0L
    private val _entries = Vector.newBuilder[Entry]
    private val _leaks = Vector.newBuilder[Leak]

    def result: Scan = Scan(_entries.result(), _leaks.result())
    def name(path: String): Unit =
      _private_tokens.filter(path.contains).foreach(token => _leaks += Leak(path, token))

    def file(path: Path, label: String): Unit = {
      name(label)
      require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), s"Missing owner file: $path")
      if (_archive(label)) {
        val file = new RandomAccessFile(path.toFile, "r")
        val members = try _central(file.length(), (offset, count) => {
          val bytes = new Array[Byte](count)
          file.seek(offset)
          file.readFully(bytes)
          bytes
        }) finally file.close()
        val stream = Files.newInputStream(path)
        try _zip(stream, members, label, 0) finally stream.close()
      } else {
        val stream = Files.newInputStream(path)
        try _payload(stream, label, false) finally stream.close()
      }
    }

    private def _payload(stream: InputStream, label: String, collect: Boolean): Array[Byte] = {
      val buffer = new Array[Byte](8192)
      val saved = if (collect) Some(new ByteArrayOutputStream) else None
      var examined = 0L
      var overlap = ""
      var found = Set.empty[String]
      var count = stream.read(buffer)
      while (count != -1) {
        examined += count
        _examined += count
        require(examined <= limits.entrybytes, s"Entry limit exceeded: $label")
        require(_examined <= limits.totalbytes, s"Boundary total limit exceeded: $label")
        val text = overlap + new String(buffer, 0, count, StandardCharsets.ISO_8859_1)
        found ++= _tokens.filter(text.contains)
        overlap = text.takeRight(_tokens.map(_.length).max - 1)
        saved.foreach(_.write(buffer, 0, count))
        count = stream.read(buffer)
      }
      _entries += Entry(label, found)
      _private_tokens.filter(found.contains).foreach(token => _leaks += Leak(label, token))
      saved.map(_.toByteArray).getOrElse(Array.emptyByteArray)
    }

    private def _zip(stream: InputStream, members: Vector[ArchiveMember], label: String, depth: Int): Unit = {
      require(depth <= limits.depth, s"Archive depth limit exceeded: $label")
      val zipstream = new ZipInputStream(stream)
      var seen = Vector.empty[String]
      try {
        var entry = zipstream.getNextEntry
        while (entry != null) {
          val path = label + "!/" + entry.getName
          name(path)
          val expected = members.find(_.name == entry.getName)
            .getOrElse(throw new IllegalArgumentException(s"Unlisted ZIP entry: $path"))
          require(!seen.contains(entry.getName), s"Duplicate ZIP entry: $path")
          seen :+= entry.getName
          require(expected.size <= limits.entrybytes, s"Declared entry limit exceeded: $path")
          val start = _examined
          val nestedarchive = !entry.isDirectory && _archive(entry.getName)
          val bytes = _payload(zipstream, path, nestedarchive)
          require(_examined - start == expected.size, s"Corrupt entry size: $path")
          zipstream.closeEntry() // ZipInputStream validates the entry's size and CRC.
          if (nestedarchive) {
            val nestedmembers = _central(bytes.length.toLong, (offset, count) =>
              bytes.slice(offset.toInt, offset.toInt + count))
            _zip(new ByteArrayInputStream(bytes), nestedmembers, path, depth + 1)
          }
          entry = zipstream.getNextEntry
        }
        require(seen.toSet == members.map(_.name).toSet, s"Incomplete ZIP traversal: $label")
      } finally zipstream.close()
    }

    private def _central(length: Long, read: (Long, Int) => Array[Byte]): Vector[ArchiveMember] = {
      require(length >= 22, "Corrupt archive: missing end directory")
      val tail = read(math.max(0L, length - 65557), math.min(length, 65557L).toInt)
      def _unsigned_(bytes: Array[Byte], offset: Int, count: Int): Long =
        (0 until count).foldLeft(0L)((value, index) => value | ((bytes(offset + index).toLong & 255L) << (8 * index)))
      val end = (tail.length - 22 to 0 by -1).find { index =>
        _unsigned_(tail, index, 4) == 0x06054b50L &&
          index + 22 + _unsigned_(tail, index + 20, 2) == tail.length
      }.getOrElse(throw new IllegalArgumentException("Corrupt archive: missing end directory"))
      require(_unsigned_(tail, end + 4, 2) == 0 && _unsigned_(tail, end + 6, 2) == 0,
        "Split ZIP archives are not exclusion evidence")
      val count = _unsigned_(tail, end + 10, 2).toInt
      require(count != 65535 && _unsigned_(tail, end + 8, 2) == count, "Unsupported or corrupt ZIP inventory")
      val size = _unsigned_(tail, end + 12, 4)
      val offset = _unsigned_(tail, end + 16, 4)
      val endoffset = length - tail.length + end
      require(offset + size == endoffset, "Corrupt or unsupported ZIP directory extent")
      var cursor = offset
      val members = Vector.newBuilder[ArchiveMember]
      (0 until count).foreach { _ =>
        require(cursor + 46 <= endoffset, "Truncated ZIP directory")
        val header = read(cursor, 46)
        require(_unsigned_(header, 0, 4) == 0x02014b50L, "Corrupt ZIP central header")
        require((_unsigned_(header, 8, 2) & 1L) == 0, "Encrypted ZIP entry is unreadable")
        val mode = (_unsigned_(header, 38, 4) >> 16) & 0xf000L
        require(mode == 0 || mode == 0x8000L || mode == 0x4000L, "ZIP symlink or special entry rejected")
        val namesize = _unsigned_(header, 28, 2).toInt
        val extent = 46 + namesize + _unsigned_(header, 30, 2).toInt + _unsigned_(header, 32, 2).toInt
        require(cursor + extent <= endoffset, "Truncated ZIP member metadata")
        val charset = if ((_unsigned_(header, 8, 2) & 2048L) != 0) StandardCharsets.UTF_8
          else java.nio.charset.Charset.forName("CP437")
        val name = new String(read(cursor + 46, namesize), charset)
        require(name.nonEmpty && !name.startsWith("/") && !name.contains('\\') &&
          !name.split('/').contains(".."), "Unsafe ZIP member name")
        val uncompressed = _unsigned_(header, 24, 4)
        require(uncompressed != 0xffffffffL, "ZIP64 requires a separately admitted scanner")
        members += ArchiveMember(name, uncompressed)
        cursor += extent
      }
      require(cursor == endoffset, "Incomplete ZIP central directory")
      val result = members.result()
      require(result.map(_.name).distinct.size == result.size, "Duplicate ZIP central names")
      result
    }
  }
}
