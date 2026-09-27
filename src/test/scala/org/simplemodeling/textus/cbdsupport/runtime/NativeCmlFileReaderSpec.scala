package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters.*

import com.sun.jna.{Library, Native, Platform}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class NativeCmlFileReaderSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private trait DarwinTestLibC extends Library {
    def mkfifo(pathname: String, mode: Int): Int
  }

  if (Platform.isMac) {
    "Native CML safe reading" should {
      "read exact regular-file bytes through the physical project-root anchor" in {
        Given("a regular CML target below a temporary project root")
        _with_root { root =>
          val bytes = "entity Customer\nattribute name\n".getBytes(StandardCharsets.UTF_8)
          _write(root.resolve("models/customer.cml"), bytes)

          When("the Darwin descriptor reader traverses the physical root and owner target path")
          val result = NativeCmlFileReader.read(root, "models/customer.cml")

          Then("the result retains the exact bytes without exposing a descriptor")
          _read_bytes(result) shouldBe bytes.toVector
        }
      }

      "reject target-directory and final-target symbolic links without exposing their external bytes" in {
        Given("a project root whose requested target paths encounter external symbolic links")
        _with_root { root =>
          val outside = Files.createTempDirectory("native-cml-reader-outside-")
          try {
            _write(outside.resolve("customer.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
            Files.createDirectories(root.resolve("links"))
            Files.createSymbolicLink(root.resolve("links/directory"), outside)
            Files.createSymbolicLink(root.resolve("links/final.cml"), outside.resolve("customer.cml"))

            When("the owner path crosses a symbolic directory or names a symbolic final target")
            val directoryresult = NativeCmlFileReader.read(root, "links/directory/customer.cml")
            val finalresult = NativeCmlFileReader.read(root, "links/final.cml")

            Then("both attempts fail closed and neither result retains the external payload")
            _is_malformed(directoryresult) shouldBe true
            _is_malformed(finalresult) shouldBe true
            directoryresult.toString should not include "entity Outside"
            finalresult.toString should not include "entity Outside"
          } finally _delete_tree(outside)
        }
      }

      "reject a symbolic final project-root component before descriptor descent" in {
        Given("a symbolic project-root path whose target is otherwise regular")
        val parent = Files.createTempDirectory("native-cml-reader-root-link-")
        try {
          val actual = parent.resolve("actual")
          val alias = parent.resolve("alias")
          _write(actual.resolve("models/customer.cml"), "entity Customer\n".getBytes(StandardCharsets.UTF_8))
          Files.createSymbolicLink(alias, actual)

          When("the reader is given the symbolic final root component")
          val result = NativeCmlFileReader.read(alias, "models/customer.cml")

          Then("the source boundary is malformed rather than followed")
          _is_malformed(result) shouldBe true
        } finally _delete_tree(parent)
      }

      "reject a symbolic ancestor of an otherwise regular project root without exposing external bytes" in {
        Given("a regular final project root beneath a symbolic ancestor that leads to external CML bytes")
        val parent = Files.createTempDirectory("native-cml-reader-root-ancestor-")
        try {
          val external = parent.resolve("external")
          val alias = parent.resolve("alias")
          val project = external.resolve("project")
          _write(project.resolve("models/customer.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
          Files.createSymbolicLink(alias, external)

          When("the reader descends from the filesystem root through the supplied ancestor components")
          val result = NativeCmlFileReader.read(alias.resolve("project"), "models/customer.cml")

          Then("the symbolic ancestor is malformed and its external payload is never retained")
          _is_malformed(result) shouldBe true
          result.toString should not include "entity Outside"
        } finally _delete_tree(parent)
      }

      "support a /var system alias through its physical project-root anchor" in {
        Given("a temporary project root addressed through /var when its physical location uses /private/var")
        _with_root { root =>
          val bytes = "entity Alias\n".getBytes(StandardCharsets.UTF_8)
          _write(root.resolve("models/alias.cml"), bytes)
          val physicalroot = root.toRealPath()
          val privatevar = Path.of("/private/var")
          val aliasroot =
            if physicalroot.startsWith(privatevar) then Path.of("/var").resolve(privatevar.relativize(physicalroot))
            else physicalroot

          When("the reader resolves the system alias before no-follow descriptor descent")
          val result = NativeCmlFileReader.read(aliasroot, "models/alias.cml")

          Then("the physical anchor preserves the exact in-root bytes")
          _read_bytes(result) shouldBe bytes.toVector
        }
      }

      "preserve missing and nonregular target outcomes without consuming bytes or hanging on a FIFO" in {
        Given("a project root with a missing target, a directory target, and a FIFO target")
        _with_root { root =>
          Files.createDirectories(root.resolve("models/directory.cml"))
          val fifo = root.resolve("models/stream.cml")
          Files.createDirectories(fifo.getParent)
          _darwin_libc.mkfifo(fifo.toString, 0x1a4) shouldBe 0

          When("the reader opens each owner target with the no-follow nonblocking final-file flags")
          val missing = NativeCmlFileReader.read(root, "models/missing.cml")
          val directory = NativeCmlFileReader.read(root, "models/directory.cml")
          val fifores = NativeCmlFileReader.read(root, "models/stream.cml")

          Then("absence is distinct while directory and FIFO targets are malformed without byte output")
          _is_unavailable(missing) shouldBe true
          _is_malformed(directory) shouldBe true
          _is_malformed(fifores) shouldBe true
          directory.toString should not include "entity"
          fifores.toString should not include "entity"
        }
      }

      "close failed traversal descriptors where the process descriptor surface is observable" in {
        Given("a symbolic final target and an observable process descriptor directory when the host exposes one")
        _with_root { root =>
          val outside = Files.createTempDirectory("native-cml-reader-fd-outside-")
          try {
            _write(outside.resolve("customer.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
            Files.createDirectories(root.resolve("links"))
            Files.createSymbolicLink(root.resolve("links/customer.cml"), outside.resolve("customer.cml"))
            val before = _descriptor_count

            When("the rejected symbolic target is attempted repeatedly")
            val results = Vector.fill(32)(NativeCmlFileReader.read(root, "links/customer.cml"))
            val after = _descriptor_count

            Then("each attempt fails closed and no accumulating descriptor leak is visible")
            results.forall(_is_malformed) shouldBe true
            before.zip(after).foreach { case (initial, finalcount) =>
              finalcount should be <= (initial + 1L)
            }
          } finally _delete_tree(outside)
        }
      }

      "capture property-generated byte variations exactly" in {
        val generated = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)

        forAll(generated) { suffix =>
          _with_root { root =>
            Given("generated ASCII CML content that differs at the byte level")
            val bytes = ("entity Generated\nattribute " + suffix + "\n").getBytes(StandardCharsets.UTF_8)
            _write(root.resolve("models/generated.cml"), bytes)

            When("the reader captures the source-owner target")
            val result = NativeCmlFileReader.read(root, "models/generated.cml")

            Then("the returned observed bytes retain the generated variation exactly")
            _read_bytes(result) shouldBe bytes.toVector
          }
        }
      }
    }
  } else {
    "Native CML safe reading" should {
      "preserve a regular temporary project root without requiring a Darwin system alias" in {
        Given("a regular consuming project root created through the host temporary-directory API")
        val root = Files.createTempDirectory("native-cml-reader-nonmac-")
        try {

          When("the shared project-root normalizer anchors the ordinary directory")
          val result = NativeCmlFileReader.normalizeProjectRoot(root)

          Then("it retains the normalized root instead of classifying the system directory as a Darwin alias")
          result shouldBe Right(root.toAbsolutePath.normalize)
        } finally _delete_tree(root)
      }

      "fail closed explicitly on an unsupported platform" in {
        Given("a non-macOS host and an otherwise arbitrary source-owner path")

        When("the Darwin-only adapter is asked to read it")
        val result = NativeCmlFileReader.read(Path.of("/unsupported-project"), "models/customer.cml")

        Then("it returns the closed malformed result without attempting a native descriptor read")
        _is_malformed(result) shouldBe true
      }
    }
  }

  private lazy val _darwin_libc: DarwinTestLibC = Native.load("c", classOf[DarwinTestLibC])

  private def _read_bytes(result: NativeCmlFileReader.Result): Vector[Byte] =
    result match {
      case NativeCmlFileReader.Result.Read(bytes) => bytes
      case _ => fail("native CML reader did not capture bytes")
    }

  private def _is_malformed(result: NativeCmlFileReader.Result): Boolean =
    result match {
      case NativeCmlFileReader.Result.Malformed(_) => true
      case _ => false
    }

  private def _is_unavailable(result: NativeCmlFileReader.Result): Boolean =
    result match {
      case NativeCmlFileReader.Result.Unavailable(_) => true
      case _ => false
    }

  private def _with_root(f: Path => Unit): Unit = {
    val root = Files.createTempDirectory("native-cml-reader-")
    try f(root)
    finally _delete_tree(root)
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes)
    ()
  }

  private def _descriptor_count: Option[Long] = {
    val descriptors = Path.of("/dev/fd")
    if Files.isDirectory(descriptors) then {
      val stream = Files.list(descriptors)
      try Some(stream.count())
      finally stream.close()
    } else None
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
