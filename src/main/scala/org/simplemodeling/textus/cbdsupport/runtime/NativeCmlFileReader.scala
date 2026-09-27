package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.{Files, LinkOption, Path}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import com.sun.jna.{Library, Memory, Native, NativeLong, Platform, Pointer}

/*
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
/** Reads one CML file through a Darwin descriptor-relative no-follow boundary. */
private[runtime] object NativeCmlFileReader {
  enum Result {
    case Read(bytes: Vector[Byte])
    case Unavailable(reason: String)
    case Unauthorized(reason: String)
    case Malformed(reason: String)
  }

  private trait DarwinLibC extends Library {
    def open(pathname: String, flags: Int): Int
    def openat(dirfd: Int, pathname: String, flags: Int): Int
    def fstat(fd: Int, stat: Pointer): Int
    def read(fd: Int, bytes: Array[Byte], count: NativeLong): NativeLong
    def close(fd: Int): Int
    def __error(): Pointer
  }

  private object DarwinLp64Stat {
    val _size: Long = 144L
    val _mode_offset: Long = 4L
  }

  private val _o_rdonly = 0x00000000
  private val _o_nonblock = 0x00000004
  private val _o_nofollow = 0x00000100
  private val _o_directory = 0x00100000
  private val _o_cloexec = 0x01000000
  private val _directory_flags = _o_directory | _o_nofollow | _o_cloexec
  private val _file_flags = _o_rdonly | _o_nofollow | _o_cloexec | _o_nonblock
  private val _eintr = 4
  private val _enoent = 2
  private val _eacces = 13
  private val _eperm = 1
  private val _s_ifmt = 0xf000
  private val _s_ifreg = 0x8000
  private val _read_buffer_size = 8192
  private val _system_root_aliases = Vector(
    Path.of("/var") -> Path.of("/private/var"),
    Path.of("/tmp") -> Path.of("/private/tmp")
  )

  private lazy val _libc: DarwinLibC = Native.load("c", classOf[DarwinLibC])

  def read(projectRoot: Path, projectRelativePath: String): Result =
    _read_on_darwin(projectRelativePath) {
      normalizeProjectRoot(projectRoot).fold(result => result, root => _read_from_physical_root(root, projectRelativePath))
    }

  private[runtime] def readPhysical(physicalProjectRoot: Path, projectRelativePath: String): Result =
    _read_on_darwin(projectRelativePath) {
      if (physicalProjectRoot == null) Result.Malformed("consuming project root is missing")
      else _read_from_physical_root(physicalProjectRoot, projectRelativePath)
    }

  private def _read_on_darwin(projectrelativepath: String)(operation: => Result): Result = {
    if (!Platform.isMac) {
      Result.Malformed("native CML safe reader is unsupported on this platform")
    } else {
      try {
        if (Native.POINTER_SIZE != 8) {
          Result.Malformed("native CML safe reader requires the Darwin LP64 ABI")
        } else if (!InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(projectrelativepath)) {
          Result.Malformed("CML current target path is unsafe")
        } else {
          operation
        }
      } catch {
        case _: LinkageError => Result.Malformed("native CML safe reader is unavailable")
        case NonFatal(_) => Result.Malformed("CML current target cannot be read safely")
      }
    }
  }

  /** Normalizes the one permitted Darwin system alias without resolving user-controlled links. */
  private[runtime] def normalizeProjectRoot(projectRoot: Path): Either[Result, Path] = {
    if (projectRoot == null) {
      Left(Result.Malformed("consuming project root is missing"))
    } else {
      try {
        val root = projectRoot.toAbsolutePath.normalize
        _normalized_system_root(root).flatMap { anchoredroot =>
          val attributes = Files.readAttributes(anchoredroot, classOf[java.nio.file.attribute.BasicFileAttributes], LinkOption.NOFOLLOW_LINKS)
          if (attributes.isSymbolicLink || Files.isSymbolicLink(anchoredroot) || !attributes.isDirectory) {
            Left(Result.Malformed("consuming project root must be a non-symbolic directory"))
          } else {
            Right(anchoredroot)
          }
        }
      } catch {
        case _: java.nio.file.AccessDeniedException => Left(Result.Unauthorized("consuming project root cannot be read"))
        case _: SecurityException => Left(Result.Unauthorized("consuming project root cannot be read"))
        case NonFatal(_) => Left(Result.Malformed("consuming project root cannot be anchored safely"))
      }
    }
  }

  private def _normalized_system_root(root: Path): Either[Result, Path] =
    if (!Platform.isMac) {
      Right(root)
    } else {
      _system_root_aliases.find { case (alias, _) => root.startsWith(alias) } match {
        case None => Right(root)
        case Some((alias, expected)) =>
          if (!Files.isSymbolicLink(alias)) {
            Left(Result.Malformed("consuming project root has an unsupported system alias"))
          } else {
            val target = Files.readSymbolicLink(alias)
            val resolved = if (target.isAbsolute) target.normalize else alias.getParent.resolve(target).normalize
            if (resolved == expected) Right(expected.resolve(alias.relativize(root)))
            else Left(Result.Malformed("consuming project root has an unsupported system alias"))
          }
      }
    }

  private def _read_from_physical_root(physicalroot: Path, projectrelativepath: String): Result = {
    val descriptors = ArrayBuffer.empty[Int]
    try {
      _open_root().flatMap { rootfd =>
        descriptors += rootfd
        _open_directories(rootfd, _path_components(physicalroot), descriptors, missingisabsence = false)
      }.flatMap { projectrootfd =>
        val components = projectrelativepath.split("/", -1).toVector
        _open_directories(projectrootfd, components.dropRight(1), descriptors, missingisabsence = true).flatMap { parentfd =>
          _open_file(parentfd, components.last).map { filefd =>
            descriptors += filefd
            filefd
          }
        }
      }.flatMap { filefd =>
        _regular_file(filefd).map(_ => _read_file(filefd))
      }.fold(result => result, result => result)
    } finally {
      descriptors.reverseIterator.foreach(_close_descriptor)
    }
  }

  private def _path_components(path: Path): Vector[String] =
    path.iterator.asScala.map(_.toString).toVector

  private def _open_root(): Either[Result, Int] =
    _retry_int(_libc.open("/", _directory_flags)).left.map(error => _error_result(error, missingisabsence = false))

  private def _open_directories(
    startfd: Int,
    components: Vector[String],
    descriptors: ArrayBuffer[Int],
    missingisabsence: Boolean
  ): Either[Result, Int] =
    components.foldLeft[Either[Result, Int]](Right(startfd)) { (result, component) =>
      result.flatMap { parentfd =>
        _retry_int(_libc.openat(parentfd, component, _directory_flags)).left.map(error => _error_result(error, missingisabsence)).map { childfd =>
          descriptors += childfd
          childfd
        }
      }
    }

  private def _open_file(parentfd: Int, target: String): Either[Result, Int] =
    _retry_int(_libc.openat(parentfd, target, _file_flags)).left.map(error => _error_result(error, missingisabsence = true))

  private def _regular_file(fd: Int): Either[Result, Unit] = {
    val stat = new Memory(DarwinLp64Stat._size)
    _retry_int(_libc.fstat(fd, stat)).left.map(error => _error_result(error, missingisabsence = false)).flatMap { _ =>
      val mode = stat.getShort(DarwinLp64Stat._mode_offset).toInt & 0xffff
      Either.cond((mode & _s_ifmt) == _s_ifreg, (), Result.Malformed("CML current target must be a regular file"))
    }
  }

  private def _read_file(fd: Int): Result = {
    val collected = new ByteArrayOutputStream()
    val buffer = new Array[Byte](_read_buffer_size)
    var result: Option[Result] = None
    var reading = true
    while (reading && result.isEmpty) {
      val count = _libc.read(fd, buffer, new NativeLong(buffer.length.toLong)).longValue
      if (count > 0 && count <= buffer.length) {
        collected.write(buffer, 0, count.toInt)
      } else if (count == 0) {
        reading = false
      } else if (count > buffer.length) {
        result = Some(Result.Malformed("CML current target cannot be read safely"))
      } else {
        val error = _native_error
        if (error != _eintr) {
          result = Some(_error_result(error, missingisabsence = false))
        }
      }
    }
    result.getOrElse(Result.Read(collected.toByteArray.toVector))
  }

  private def _retry_int(operation: => Int): Either[Int, Int] = {
    var value = operation
    var error = if (value < 0) _native_error else 0
    while (value < 0 && error == _eintr) {
      value = operation
      error = if (value < 0) _native_error else 0
    }
    if (value < 0) Left(error) else Right(value)
  }

  private def _native_error: Int = {
    val pointer = _libc.__error()
    if (pointer == null) 0 else pointer.getInt(0L)
  }

  private def _error_result(error: Int, missingisabsence: Boolean): Result =
    if (missingisabsence && error == _enoent) Result.Unavailable("CML current target is absent")
    else if (error == _eacces || error == _eperm) Result.Unauthorized("CML current target cannot be read")
    else Result.Malformed("CML current target cannot be read safely")

  private def _close_descriptor(fd: Int): Unit = {
    try {
      _libc.close(fd)
      ()
    } catch {
      case _: LinkageError => ()
      case NonFatal(_) => ()
    }
  }
}
