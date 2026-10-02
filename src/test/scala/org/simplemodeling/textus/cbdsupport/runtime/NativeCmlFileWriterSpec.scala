package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import scala.jdk.CollectionConverters.*
import com.sun.jna.{Library, Native, NativeLong, Platform, Pointer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Real existing-file effects, anchored preflight and bounded syscall failures.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
final class NativeCmlFileWriterSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import NativeCmlFileWriter.*
  import Disposition.*

  private trait DarwinTestLibC extends Library {
    def mkfifo(pathname: String, mode: Int): Int
    def fcntl(fd: Int, command: Int): Int
    def __error(): Pointer
  }
  private lazy val _libc: DarwinTestLibC = Native.load("c", classOf[DarwinTestLibC])
  private val _original = Vector[Byte](10, 11, 12, 13, 14)
  private val _targets = Vector(Target("alpha", "cml/alpha.cml", Vector[Byte](1, 2)),
    Target("beta", "cml/beta.cml", Vector[Byte](3, 4, 5, 6, 7, 8)),
    Target("gamma", "cml/gamma.cml", Vector.empty))

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 8)

  "Existing native CML replacement" should {
    "structural admission" which {
      "reject missing duplicate and unsafe inputs before any file effect" in {
        Given("existing files and malformed null, empty, duplicate and unsafe target declarations")
        _with_root { root =>
          val variants = Vector[Vector[Target]](null, Vector.empty, Vector(null),
            Vector(_targets.head.copy(targetid = " ")),
            Vector(_targets.head.copy(bytes = null)),
            Vector(_targets.head, _targets.head.copy(projectrelativepath = "cml/beta.cml")),
            Vector(_targets.head, _targets(1).copy(projectrelativepath = "cml/alpha.cml"))) ++
            Vector("", "/outside.cml", "../outside.cml", "cml/../alpha.cml", "cml//alpha.cml", "cml/./alpha.cml", "cml/a\u0000.cml")
              .map(path => Vector(_targets.head.copy(projectrelativepath = path)))
          When("existing-only replacement receives each malformed declaration or missing root")
          val reports = variants.map(replaceExisting(root, _)) :+ replaceExisting(null, _targets)
          Then("every report fails with no attempted mutation and original bytes survive")
          reports.foreach { report =>
            report.failure.get.kind shouldBe FailureKind.Malformed
            report.outcomes.forall(outcome => outcome.disposition == NotAttempted && !outcome.mayhavechanged && outcome.byteswritten == 0) shouldBe true
          }
          _unchanged(root)
        }
      }
    }
  }

  if (Platform.isMac && Native.POINTER_SIZE == 8 && NativeLong.SIZE == 8) {
    "Existing native CML replacement" should {
      "complete physical application" which {
        "replace shorter longer and empty payloads in order while retaining mode and owner" in {
          Given("three existing regular files with explicit original mode and ownership")
          _with_root { root =>
            val paths = _targets.map(target => root.resolve(target.projectrelativepath))
            Files.setPosixFilePermissions(paths.head, PosixFilePermissions.fromString("rw-r-----"))
            val metadata = paths.map(path => (Files.getPosixFilePermissions(path), Files.getOwner(path)))
            val before = _descriptor_count
            When("the native writer applies the complete existing target set")
            val report = replaceExisting(root, _targets)
            val after = _descriptor_count
            Then("exact output, ordered successful effects and closed resources preserve existing metadata")
            report.failure shouldBe None
            report.cleanupfailures shouldBe empty
            report.outcomes.map(_.disposition) shouldBe Vector(Applied, Applied, Applied)
            report.outcomes.map(_.targetid) shouldBe _targets.map(_.targetid)
            report.outcomes.map(_.byteswritten) shouldBe _targets.map(_.bytes.size.toLong)
            report.outcomes.forall(_.mayhavechanged) shouldBe true
            paths.map(path => Files.readAllBytes(path).toVector) shouldBe _targets.map(_.bytes)
            paths.map(path => (Files.getPosixFilePermissions(path), Files.getOwner(path))) shouldBe metadata
            after should be <= (before + 1L)
          }
        }

        "retain property-generated bounded Unicode and binary output exactly" in {
          Given("bounded Unicode strings and independent arbitrary binary payloads")
          val unicode = Gen.listOfN(12, Gen.oneOf('a', 'é', '漢', '\n', '\u0000')).map(_.mkString.getBytes(StandardCharsets.UTF_8).toVector)
          val binary = Gen.choose(0, 64).flatMap(size => Gen.listOfN(size, Gen.choose(-128, 127).map(_.toByte))).map(_.toVector)
          forAll(unicode, binary) { (text, bytes) =>
            _with_root { root =>
              val targets = Vector(_targets.head.copy(bytes = text), _targets(1).copy(bytes = bytes))
              When("the real writer replaces both generated payloads")
              val report = replaceExisting(root, targets)
              Then("the file bytes and applied counts retain both generated values exactly")
              report.failure shouldBe None
              report.cleanupfailures shouldBe empty
              report.outcomes.map(_.disposition) shouldBe Vector(Applied, Applied)
              targets.foreach(target => Files.readAllBytes(root.resolve(target.projectrelativepath)).toVector shouldBe target.bytes)
            }
          }
        }

        "normalize only the established var and tmp system aliases" in {
          Given("the existing Darwin system aliases and the exact normalizer reused by the writer")
          Vector(Path.of("/var") -> Path.of("/private/var"), Path.of("/tmp") -> Path.of("/private/tmp")).foreach { case (alias, physical) =>
            When("the writer's shared root normalizer anchors an established system alias")
            val result = NativeCmlFileReader.normalizeProjectRoot(alias)
            Then("the alias resolves only to its specified physical root without creating work outside target")
            result shouldBe Right(physical)
          }
        }
      }

      "all-target preflight" which {
        "leave the first target untouched when the second is absent unwritable directory or FIFO" in {
          Given("a valid first target followed by one unusable existing-target declaration")
          Vector("missing", "unwritable", "directory", "fifo").foreach { kind =>
            _with_root { root =>
              val second = root.resolve("cml/beta.cml")
              if (kind == "unwritable") Files.setPosixFilePermissions(second, PosixFilePermissions.fromString("---------"))
              else {
                Files.delete(second)
                if (kind == "directory") Files.createDirectory(second)
                if (kind == "fifo") _libc.mkfifo(second.toString, 0x180) shouldBe 0
              }
              val before = _descriptor_count
              When("the writer preflights all targets without creation or early truncation")
              val report = replaceExisting(root, _targets)
              val after = _descriptor_count
              Then("the entire set remains unattempted, FIFO does not hang, and the first bytes remain")
              report.failure should not be empty
              report.cleanupfailures shouldBe empty
              report.outcomes.map(_.disposition) shouldBe Vector(NotAttempted, NotAttempted, NotAttempted)
              report.outcomes.forall(outcome => outcome.byteswritten == 0L && !outcome.mayhavechanged) shouldBe true
              Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe _original
              Files.readAllBytes(root.resolve("cml/gamma.cml")).toVector shouldBe _original
              if (kind == "missing") Files.exists(second) shouldBe false
              if (kind == "unwritable") {
                report.failure.get.kind shouldBe FailureKind.Unauthorized
                Files.setPosixFilePermissions(second, PosixFilePermissions.fromString("rw-------"))
                Files.readAllBytes(second).toVector shouldBe _original
              }
              after should be <= (before + 1L)
            }
          }
        }

        "reject final parent root and ancestor symlinks without changing outside files" in {
          Given("newly owned fixture subtrees with symbolic boundaries leading to outside bytes")
          Vector("final", "parent", "root", "ancestor").foreach { kind =>
            _with_root { root =>
              val outside = root.resolve("outside")
              Files.createDirectories(outside.resolve("project/cml"))
              Files.write(outside.resolve("project/cml/beta.cml"), _original.toArray)
              var project = root
              var targets = _targets
              kind match {
                case "final" =>
                  Files.delete(root.resolve("cml/beta.cml"))
                  Files.createSymbolicLink(root.resolve("cml/beta.cml"), outside.resolve("project/cml/beta.cml"))
                case "parent" =>
                  Files.createSymbolicLink(root.resolve("alias"), outside.resolve("project/cml"))
                  targets = _targets.updated(1, _targets(1).copy(projectrelativepath = "alias/beta.cml"))
                case "root" =>
                  Files.createSymbolicLink(root.resolve("alias"), outside.resolve("project"))
                  project = root.resolve("alias")
                case _ =>
                  Files.createSymbolicLink(root.resolve("alias"), outside)
                  project = root.resolve("alias/project")
              }
              When("anchored existing-target preflight encounters the symbolic boundary")
              val report = replaceExisting(project, targets)
              Then("all targets remain unattempted and both original and outside bytes are untouched")
              report.failure should not be empty
              report.outcomes.map(_.disposition) shouldBe Vector(NotAttempted, NotAttempted, NotAttempted)
              Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe _original
              Files.readAllBytes(outside.resolve("project/cml/beta.cml")).toVector shouldBe _original
            }
          }
        }

        "reject hardlinked target ownership before any mutation" in {
          Given("a valid first target and a second with another physical link in the owned outside subtree")
          _with_root { root =>
            Files.createLink(root.resolve("outside.cml"), root.resolve("cml/beta.cml"))
            When("the writer checks the unsigned native link count of every opened target")
            val report = replaceExisting(root, _targets)
            Then("the entire set is unattempted and every physical name retains its original bytes")
            report.failure.get.kind shouldBe FailureKind.Malformed
            report.failure.get.diagnostic should include("link")
            report.outcomes.map(_.disposition) shouldBe Vector(NotAttempted, NotAttempted, NotAttempted)
            _unchanged(root)
            Files.readAllBytes(root.resolve("outside.cml")).toVector shouldBe _original
          }
        }

        "distinguish case-spelling physical aliases from truly distinct case-sensitive files" in {
          Given("the actual filesystem's case-spelling behavior for two distinct declared paths")
          _with_root { root =>
            val other = root.resolve("cml/ALPHA.cml")
            val alias = Files.exists(other)
            if (!alias) Files.write(other, _original.toArray)
            val targets = Vector(_targets.head, Target("case-spelling", "cml/ALPHA.cml", Vector[Byte](9)))
            When("the writer compares only transient native physical ownership observations")
            val report = replaceExisting(root, targets)
            Then("an actual physical alias rejects preflight while distinct physical files can both apply")
            if (alias) {
              report.failure.get.kind shouldBe FailureKind.Malformed
              report.failure.get.diagnostic should include("alias")
              report.outcomes.map(_.disposition) shouldBe Vector(NotAttempted, NotAttempted)
              _unchanged(root)
            } else {
              report.failure shouldBe None
              report.outcomes.map(_.disposition) shouldBe Vector(Applied, Applied)
              Files.readAllBytes(other).toVector shouldBe Vector[Byte](9)
              Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe _targets.head.bytes
            }
          }
        }
      }

      "individual syscall outcomes" which {
        "preserve a real applied prefix failed second target and untouched later target" in {
          Given("three native-preflighted targets and a forwarding second-target write failure")
          _with_root { root =>
            val seen = scala.collection.mutable.ArrayBuffer.empty[Int]
            var writes = 0
            val operations = new ForwardingOperations {
              override def truncate(fd: Int): Either[Failure, Unit] = { seen += fd; super.truncate(fd) }
              override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = {
                writes += 1
                if (writes == 2) Left(_fault("second-write")) else super.write(fd, bytes)
              }
            }
            val before = _descriptor_count
            When("actual first-target writes are forwarded and only the second write syscall fails")
            val report = replaceExistingWithOperations(root, _targets, operations)
            val after = _descriptor_count
            Then("actual prefix effects, precise failure and all later unattempted resources survive")
            report.outcomes.map(_.disposition) shouldBe Vector(Applied, Failed, NotAttempted)
            report.outcomes.map(_.byteswritten) shouldBe Vector(2L, 0L, 0L)
            report.outcomes.map(_.mayhavechanged) shouldBe Vector(true, true, false)
            report.failure shouldBe Some(_fault("second-write"))
            report.outcomes(1).failure shouldBe report.failure
            report.cleanupfailures shouldBe empty
            Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe _targets.head.bytes
            Files.readAllBytes(root.resolve("cml/beta.cml")).toVector shouldBe Vector.empty
            Files.readAllBytes(root.resolve("cml/gamma.cml")).toVector shouldBe _original
            seen.foreach(_closed)
            after should be <= (before + 1L)
          }
        }

        "continue real positive short writes until the complete payload is written" in {
          Given("a forwarding decorator that writes at most one real byte per syscall")
          _with_root { root =>
            val operations = new ForwardingOperations {
              override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = super.write(fd, bytes.take(1))
            }
            When("the writer receives positive short counts from actual native writes")
            val report = replaceExistingWithOperations(root, _targets, operations)
            Then("every payload is complete with its exact total count")
            report.failure shouldBe None
            report.outcomes.map(_.disposition) shouldBe Vector(Applied, Applied, Applied)
            report.outcomes.map(_.byteswritten) shouldBe _targets.map(_.bytes.size.toLong)
            _targets.foreach(target => Files.readAllBytes(root.resolve(target.projectrelativepath)).toVector shouldBe target.bytes)
          }
        }

        "resume only each interrupted truncate write and flush syscall" in {
          Given("a forwarding decorator returning one explicit EINTR for each mutation syscall family")
          _with_root { root =>
            var truncates = 0
            var writes = 0
            var flushes = 0
            val interrupted = Failure(FailureKind.IoFailure, Some(4), "interrupted")
            val operations = new ForwardingOperations {
              override def truncate(fd: Int): Either[Failure, Unit] = {
                truncates += 1
                if (truncates == 1) Left(interrupted) else super.truncate(fd)
              }
              override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = {
                writes += 1
                if (writes == 1) Left(interrupted) else super.write(fd, bytes)
              }
              override def flush(fd: Int): Either[Failure, Unit] = {
                flushes += 1
                if (flushes == 1) Left(interrupted) else super.flush(fd)
              }
            }
            When("the writer resumes the interrupted individual calls and forwards their successors")
            val report = replaceExistingWithOperations(root, _targets.take(1), operations)
            Then("one application completes without repeating preflight or restarting any completed stage")
            report.failure shouldBe None
            report.outcomes.head.disposition shouldBe Applied
            Vector(truncates, writes, flushes) shouldBe Vector(2, 2, 2)
            Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe _targets.head.bytes
          }
        }

        "reject zero negative and oversized counts after truncation without pretending progress" in {
          Given("one preflighted target and explicit unusable write-count results")
          Vector(0L, -1L, 99L).foreach { count =>
            _with_root { root =>
              val operations = new ForwardingOperations {
                override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = Right(count)
              }
              When("the narrowed write syscall returns an invalid count")
              val report = replaceExistingWithOperations(root, _targets, operations)
              Then("the failing target retains zero progress and possible change while later targets remain original")
              report.outcomes.map(_.disposition) shouldBe Vector(Failed, NotAttempted, NotAttempted)
              report.outcomes.head.byteswritten shouldBe 0L
              report.outcomes.head.mayhavechanged shouldBe true
              report.failure.get.kind shouldBe FailureKind.IoFailure
              Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe Vector.empty
              Files.readAllBytes(root.resolve("cml/beta.cml")).toVector shouldBe _original
            }
          }
        }

        "retain truncate and flush failures with conservative change and precise written counts" in {
          Given("real targets and a single noninterrupted failure at truncate or flush")
          Vector("truncate", "flush").foreach { stage =>
            _with_root { root =>
              var attempts = 0
              val operations = new ForwardingOperations {
                override def truncate(fd: Int): Either[Failure, Unit] = {
                  if (stage == "truncate") { attempts += 1; Left(_fault(stage)) } else super.truncate(fd)
                }
                override def flush(fd: Int): Either[Failure, Unit] = {
                  if (stage == "flush") { attempts += 1; Left(_fault(stage)) } else super.flush(fd)
                }
              }
              When("the actual syscall sequence reaches the selected single failure")
              val report = replaceExistingWithOperations(root, _targets, operations)
              Then("the cause is retained without workflow retry and physical effects remain exact")
              attempts shouldBe 1
              report.failure shouldBe Some(_fault(stage))
              report.outcomes.map(_.disposition) shouldBe Vector(Failed, NotAttempted, NotAttempted)
              report.outcomes.head.mayhavechanged shouldBe true
              report.outcomes.head.byteswritten shouldBe (if (stage == "flush") 2L else 0L)
              Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe (if (stage == "flush") _targets.head.bytes else _original)
              Files.readAllBytes(root.resolve("cml/beta.cml")).toVector shouldBe _original
            }
          }
        }

        "retain positive prefix progress when a later write syscall fails" in {
          Given("a real one-byte first write followed by an explicit noninterrupted I/O failure")
          _with_root { root =>
            var calls = 0
            val operations = new ForwardingOperations {
              override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = {
                calls += 1
                if (calls == 1) super.write(fd, bytes.take(1)) else Left(_fault("after-prefix"))
              }
            }
            When("the writer records short progress and the next syscall fails")
            val report = replaceExistingWithOperations(root, _targets, operations)
            Then("one physical byte and its exact count survive with no later mutation")
            report.outcomes.head.byteswritten shouldBe 1L
            report.outcomes.head.disposition shouldBe Failed
            report.failure shouldBe Some(_fault("after-prefix"))
            Files.readAllBytes(root.resolve("cml/alpha.cml")).toVector shouldBe Vector[Byte](1)
            Files.readAllBytes(root.resolve("cml/beta.cml")).toVector shouldBe _original
          }
        }

      }
    }
  } else {
    "Existing native CML replacement" should {
      "unsupported host contract" which {
        "report explicit unsupported platform or ABI without a file effect" in {
          Given("this actual host does not supply macOS LP64 and existing files remain available")
          _with_root { root =>
            When("the production native entry is invoked on this unsupported host")
            val report = replaceExisting(root, _targets)
            Then("unsupported is an explicit failure and every target remains unattempted")
            report.failure.get.kind shouldBe FailureKind.Unsupported
            report.outcomes.map(_.disposition) shouldBe Vector(NotAttempted, NotAttempted, NotAttempted)
            _unchanged(root)
          }
        }
      }
    }
  }

  private class ForwardingOperations extends WriteOperations {
    override def truncate(fd: Int): Either[Failure, Unit] = nativeOperations.truncate(fd)
    override def write(fd: Int, bytes: Vector[Byte]): Either[Failure, Long] = nativeOperations.write(fd, bytes)
    override def flush(fd: Int): Either[Failure, Unit] = nativeOperations.flush(fd)
  }
  private def _closed(fd: Int): Unit = {
    _libc.fcntl(fd, 1) shouldBe -1
    _libc.__error().getInt(0L) shouldBe 9
  }
  private def _fault(diagnostic: String): Failure = Failure(FailureKind.IoFailure, Some(5), diagnostic)
  private def _with_root(body: Path => Unit): Unit = {
    val parent = Path.of("target/native-cml-writer/work").toAbsolutePath
    Files.createDirectories(parent)
    val root = Files.createTempDirectory(parent, "owned-")
    try { _seed(root); body(root) } finally _delete_tree(root)
  }
  private def _seed(root: Path): Unit = {
    Files.createDirectories(root.resolve("cml"))
    _targets.foreach(target => Files.write(root.resolve(target.projectrelativepath), _original.toArray))
  }
  private def _unchanged(root: Path): Unit =
    _targets.foreach(target => Files.readAllBytes(root.resolve(target.projectrelativepath)).toVector shouldBe _original)
  private def _descriptor_count: Long = {
    val stream = Files.list(Path.of("/dev/fd"))
    try stream.count() finally stream.close()
  }
  private def _delete_tree(root: Path): Unit = {
    val stream = Files.walk(root)
    val paths = try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse finally stream.close()
    paths.foreach(Files.delete)
  }
}
