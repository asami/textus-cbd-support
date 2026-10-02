package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import com.sun.jna.{Library, Memory, Native, NativeLong, Platform, Pointer}

/**
 * Existing-only ordered CML effects after complete anchored native preflight.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object NativeCmlFileWriter {
  enum Disposition {
    case Applied, Failed, NotAttempted
  }
  enum FailureKind {
    case Malformed, Unavailable, Unauthorized, Unsupported, IoFailure
  }
  final case class Target(targetid: String, projectrelativepath: String, bytes: Vector[Byte])
  final case class Failure(kind: FailureKind, errno: Option[Int], diagnostic: String)
  final case class TargetOutcome(targetid: String, projectrelativepath: String,
    disposition: Disposition, byteswritten: Long, mayhavechanged: Boolean, failure: Option[Failure])
  final case class Report(outcomes: Vector[TargetOutcome], failure: Option[Failure], cleanupfailures: Vector[Failure])

  /** Only individual mutation syscalls are substitutable; opening/stat/close stay native. */
  private[runtime] trait WriteOperations {
    def truncate(fd: Int): Either[Failure, Unit]
    def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long]
    def flush(fd: Int): Either[Failure, Unit]
  }

  private trait DarwinLibC extends Library {
    def open(pathname: String, flags: Int): Int
    def openat(dirfd: Int, pathname: String, flags: Int): Int
    def fstat(fd: Int, stat: Pointer): Int
    def ftruncate(fd: Int, length: Long): Int
    def write(fd: Int, bytes: Array[Byte], count: NativeLong): NativeLong
    def fsync(fd: Int): Int
    def close(fd: Int): Int
    def __error(): Pointer
  }
  private final class Descriptor(val fd: Int) {
    var owned: Boolean = true
  }
  private final case class Prepared(target: Target, descriptor: Descriptor, device: Int, inode: Long)

  private val _directory_flags = 0x00100000 | 0x00000100 | 0x01000000
  private val _file_flags = 0x00000001 | 0x00000100 | 0x01000000 | 0x00000004
  private val _eintr = 4
  private lazy val _libc: DarwinLibC = Native.load("c", classOf[DarwinLibC])

  private[runtime] val nativeOperations: WriteOperations = new WriteOperations {
    override def truncate(fd: Int): Either[Failure, Unit] =
      _native_zero("truncate")(_libc.ftruncate(fd, 0L))
    override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = _capture {
      val count = _libc.write(fd, bytes.toArray, new NativeLong(bytes.size.toLong)).longValue
      if (count < 0) Left(_errno_failure(_native_error, "write")) else Right(count)
    }
    override def flush(fd: Int): Either[Failure, Unit] =
      _native_zero("flush")(_libc.fsync(fd))
  }

  def replaceExisting(projectRoot: Path, targets: Vector[Target]): Report =
    replaceExistingWithOperations(projectRoot, targets, nativeOperations)

  private[runtime] def replaceExistingWithOperations(projectRoot: Path, targets: Vector[Target],
    operations: WriteOperations): Report = {
    val supplied = Option(targets).getOrElse(Vector.empty)
    var outcomes = supplied.map { target =>
      TargetOutcome(if (target == null) null else target.targetid,
        if (target == null) null else target.projectrelativepath, Disposition.NotAttempted, 0L, false, None)
    }
    val descriptors = ArrayBuffer.empty[Descriptor]
    val cleanup = ArrayBuffer.empty[Failure]
    var failure: Option[Failure] = None
    try {
      val prepared = for {
        _ <- _validate(projectRoot, targets, operations)
        _ <- _supported
        root <- NativeCmlFileReader.normalizeProjectRoot(projectRoot).left.map(_reader_failure)
        rootfd <- _open(_libc.open("/", _directory_flags), descriptors, "open filesystem root")
        projectfd <- _directories(rootfd, root.iterator.asScala.map(_.toString).toVector, descriptors)
        files <- _prepare_targets(projectfd, supplied, descriptors)
      } yield files
      prepared match {
        case Left(problem) => failure = Some(problem)
        case Right(files) =>
          var index = 0
          while (index < files.size && failure.isEmpty) {
            val outcome = _mutate(files(index), operations)
            outcomes = outcomes.updated(index, outcome)
            failure = outcome.failure
            index += 1
          }
      }
    } catch {
      case _: LinkageError => failure = Some(_unsupported("native CML writer is unavailable"))
      case NonFatal(_) => failure = Some(_io_failure("native CML preflight cannot complete"))
    } finally {
      descriptors.reverseIterator.filter(_.owned).foreach { descriptor =>
        _close(descriptor).left.foreach(cleanup += _)
      }
    }
    Report(outcomes, failure, cleanup.toVector)
  }

  private def _validate(root: Path, targets: Vector[Target], operations: WriteOperations): Either[Failure, Unit] = {
    val valid = root != null && targets != null && targets.nonEmpty && operations != null &&
      targets.forall(target => target != null && _nonblank(target.targetid) && target.bytes != null &&
        InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(target.projectrelativepath))
    if (!valid) Left(_malformed("root and unique existing target IDs, safe paths and byte vectors are required"))
    else if (targets.map(_.targetid).distinct.size != targets.size ||
      targets.map(_.projectrelativepath).distinct.size != targets.size)
      Left(_malformed("target IDs and paths must be unique"))
    else Right(())
  }

  private def _supported: Either[Failure, Unit] = {
    if (!Platform.isMac) Left(_unsupported("native CML writer requires macOS"))
    else if (Native.POINTER_SIZE != 8 || NativeLong.SIZE != 8)
      Left(_unsupported("native CML writer requires Darwin LP64"))
    else Right(())
  }

  private def _directories(start: Descriptor, components: Vector[String],
    descriptors: ArrayBuffer[Descriptor]): Either[Failure, Descriptor] =
    components.foldLeft[Either[Failure, Descriptor]](Right(start)) { (result, component) =>
      result.flatMap(parent => _open(_libc.openat(parent.fd, component, _directory_flags), descriptors, "open directory"))
    }

  private def _open(operation: => Int, descriptors: ArrayBuffer[Descriptor],
    diagnostic: String): Either[Failure, Descriptor] =
    _native_int(diagnostic)(operation).map { fd =>
      val descriptor = new Descriptor(fd)
      descriptors += descriptor
      descriptor
    }

  private def _prepare_targets(root: Descriptor, targets: Vector[Target],
    descriptors: ArrayBuffer[Descriptor]): Either[Failure, Vector[Prepared]] =
    targets.foldLeft[Either[Failure, Vector[Prepared]]](Right(Vector.empty)) { (result, target) =>
      result.flatMap { prior =>
        val components = target.projectrelativepath.split("/", -1).toVector
        for {
          parent <- _directories(root, components.dropRight(1), descriptors)
          descriptor <- _open(_libc.openat(parent.fd, components.last, _file_flags), descriptors, "open existing target")
          identity <- _regular_identity(descriptor.fd)
          _ <- Either.cond(!prior.exists(file => file.device == identity._1 && file.inode == identity._2), (),
            _malformed("targets alias the same physical file"))
        } yield prior :+ Prepared(target, descriptor, identity._1, identity._2)
      }
    }

  private def _regular_identity(fd: Int): Either[Failure, (Int, Long)] = {
    val stat = new Memory(144L)
    try {
      _native_zero("stat opened target")(_libc.fstat(fd, stat)).flatMap { _ =>
        val mode = stat.getShort(4L).toInt & 0xffff
        val links = stat.getShort(6L).toInt & 0xffff
        if ((mode & 0xf000) != 0x8000) Left(_malformed("target must be a regular file"))
        else if (links != 1) Left(_malformed("target must have exactly one physical link"))
        else Right((stat.getInt(0L), stat.getLong(8L)))
      }
    } finally stat.close()
  }

  private def _mutate(file: Prepared, operations: WriteOperations): TargetOutcome = {
    var written = 0L
    var mayhavechanged = false
    var failure: Option[Failure] = None
    try {
      mayhavechanged = true
      _resume(operations.truncate(file.descriptor.fd)) match {
        case Left(problem) => failure = Some(problem)
        case Right(_) =>
          while (written < file.target.bytes.size && failure.isEmpty) {
            val remaining = file.target.bytes.drop(written.toInt)
            _resume(operations.write(file.descriptor.fd, remaining)) match {
              case Left(problem) => failure = Some(problem)
              case Right(count) if count > 0 && count <= remaining.size => written += count
              case Right(_) => failure = Some(_io_failure("write returned a zero, negative or oversized count"))
            }
          }
          if (failure.isEmpty) _resume(operations.flush(file.descriptor.fd)).left.foreach(problem => failure = Some(problem))
          if (failure.isEmpty) _close(file.descriptor).left.foreach(problem => failure = Some(problem))
      }
    } catch {
      case _: LinkageError => failure = Some(_unsupported("native mutation syscall is unavailable"))
      case NonFatal(_) => failure = Some(_io_failure("mutation syscall cannot complete"))
    }
    TargetOutcome(file.target.targetid, file.target.projectrelativepath,
      if (failure.isEmpty) Disposition.Applied else Disposition.Failed, written, mayhavechanged, failure)
  }

  private def _resume[A](operation: => Either[Failure, A]): Either[Failure, A] = {
    var result = operation
    while (result.left.toOption.exists(_.errno.contains(_eintr))) result = operation
    result
  }

  private def _close(descriptor: Descriptor): Either[Failure, Unit] = {
    descriptor.owned = false
    _native_zero("close descriptor")(_libc.close(descriptor.fd))
  }

  private def _native_zero(diagnostic: String)(operation: => Int): Either[Failure, Unit] =
    _native_int(diagnostic)(operation).flatMap(result =>
      Either.cond(result == 0, (), _io_failure(diagnostic + " returned an invalid success code")))

  private def _native_int(diagnostic: String)(operation: => Int): Either[Failure, Int] = _capture {
    val result = operation
    if (result < 0) Left(_errno_failure(_native_error, diagnostic)) else Right(result)
  }

  private def _capture[A](operation: => Either[Failure, A]): Either[Failure, A] = {
    try operation
    catch {
      case _: LinkageError => Left(_unsupported("native CML writer is unavailable"))
      case NonFatal(_) => Left(_io_failure("native CML operation cannot complete"))
    }
  }

  private def _native_error: Int = {
    val pointer = _libc.__error()
    if (pointer == null) 0 else pointer.getInt(0L)
  }

  private def _errno_failure(error: Int, diagnostic: String): Failure = {
    val kind = error match {
      case 2 => FailureKind.Unavailable
      case 1 | 13 => FailureKind.Unauthorized
      case 20 | 21 | 22 | 62 => FailureKind.Malformed
      case _ => FailureKind.IoFailure
    }
    Failure(kind, Some(error), diagnostic + " failed")
  }

  private def _reader_failure(result: NativeCmlFileReader.Result): Failure = result match {
    case NativeCmlFileReader.Result.Unavailable(reason) => Failure(FailureKind.Unavailable, None, reason)
    case NativeCmlFileReader.Result.Unauthorized(reason) => Failure(FailureKind.Unauthorized, None, reason)
    case NativeCmlFileReader.Result.Malformed(reason) => _malformed(reason)
    case NativeCmlFileReader.Result.Read(_) => _malformed("root normalizer returned a payload")
  }
  private def _nonblank(value: String): Boolean = value != null && !value.isBlank
  private def _malformed(diagnostic: String): Failure = Failure(FailureKind.Malformed, None, diagnostic)
  private def _unsupported(diagnostic: String): Failure = Failure(FailureKind.Unsupported, None, diagnostic)
  private def _io_failure(diagnostic: String): Failure = Failure(FailureKind.IoFailure, None, diagnostic)
}
