import java.util.Properties
import java.nio.file.{Files, LinkOption}
import org.goldenport.cozy.CozyPlugin.autoImport._
import org.goldenport.cozy.CozyProjectIdentityEvidence
import sbt._
import sbt.Keys._

/*
 * @since   Oct.  4, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
object InternalModelExclusionBuild {
  val selectedCncfSourceRoot = settingKey[Option[File]]("Explicit CNCF source project shared by root and fixture")
  val selectedCncfSourceJars = taskKey[Seq[Attributed[File]]]("Actual selected CNCF source package JARs")
  val prepareInternalModelExclusionEvidence = taskKey[File]("Locate actual standard fixture outputs for exclusion specifications")
  private val _identity = SettingKey[CozyProjectIdentityEvidence](
    "internalModelExclusionIdentity", "Admitted fixture project identity")

  val sourceDependencySettings: Seq[Def.Setting[_]] = Seq(
    selectedCncfSourceJars := Def.taskDyn {
      selectedCncfSourceRoot.value match {
        case None => Def.task { Seq.empty[Attributed[File]] }
        case Some(directory) => Def.task {
          val expected = CbdSupportProjectYamlBuild.dependencyVersion(
            cozyProjectMetadata.value, "org.goldenport", "goldenport-cncf")
          val observed = (RootProject(directory.toURI) / version).value
          require(observed == expected, "Effective CNCF source version mismatch")
          require((RootProject(directory.toURI) / Compile / exportJars).value,
            "Selected CNCF source requires packaged Compile exports")
          val jar = (RootProject(directory.toURI) / Compile / packageBin).value
          require(Files.isRegularFile(jar.toPath, LinkOption.NOFOLLOW_LINKS) && jar.getName.endsWith(".jar"),
            "Actual regular CNCF source package JAR is required")
          val products = (RootProject(directory.toURI) / Compile / exportedProducts).value
          require(products.map(_.data.toPath.toAbsolutePath.normalize) == Seq(jar.toPath.toAbsolutePath.normalize),
            "Selected CNCF source must export exactly its actual package JAR")
          products
        }
      }
    }.value,
    Compile / dependencyClasspath := {
      val classpath = (Compile / dependencyClasspath).value
      val selected = selectedCncfSourceJars.value
      selected.foreach { jar =>
        require(classpath.exists(entry => entry.data.toPath.toAbsolutePath.normalize == jar.data.toPath.toAbsolutePath.normalize),
          "Selected CNCF source package JAR is absent from effective dependency classpath")
      }
      classpath
    }
  )

  val settings: Seq[Def.Setting[_]] = Seq(
    selectedCncfSourceRoot := None,
    _identity := CbdSupportProjectYamlBuild.admitted(cozyProjectMetadata.value, scalaBinaryVersion.value),
    organization := CbdSupportProjectYamlBuild.organization(_identity.value),
    moduleName := CbdSupportProjectYamlBuild.moduleName(_identity.value),
    name := moduleName.value,
    version := CbdSupportProjectYamlBuild.version(_identity.value),
    scalaVersion := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.scalaVersion"),
    useCoursier := false,
    resolvers += Resolver.defaultLocal,
    resolvers += Resolver.file("Local Ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    resolvers += "Local Maven Repository" at ("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven",
    libraryDependencies ++= {
      val sourceselected = selectedCncfSourceRoot.value.nonEmpty
      CbdSupportProjectYamlBuild.dependencies(cozyProjectMetadata.value).filterNot { dependency =>
        sourceselected && dependency.organization == "org.goldenport" && dependency.name == "goldenport-cncf"
      }
    },
    cozyGeneratorBackend := "cozy",
    cozyDelegateProjectDir := (LocalRootProject / cozyDelegateProjectDir).value,
    cozyDelegateCommand := Seq("cozy", "--runtime",
      CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.cozyVersion")),
    cozyCarName := CbdSupportProjectYamlBuild.carBaseName(_identity.value),
    cozyManifestMetadata ++= cozyProjectMetadata.value.mapUnder("packaging.car.manifest_metadata") ++
      CbdSupportProjectYamlBuild.manifestMetadata(_identity.value),
    publish / skip := true,
    publishLocal / skip := true,
    prepareInternalModelExclusionEvidence := Def.taskDyn {
      val fixture = cozyProjectMetadata.value
      val selected = (LocalRootProject / cozyProjectMetadata).value
      Seq("build.scalaVersion", "build.cozyVersion").foreach { key =>
        require(CbdSupportProjectYamlBuild.requiredValue(fixture, key) ==
          CbdSupportProjectYamlBuild.requiredValue(selected, key), s"Fixture configuration mismatch: $key")
      }
      require(scalaVersion.value == (LocalRootProject / scalaVersion).value, "Effective Scala version mismatch")
      require(cozyDelegateCommand.value == (LocalRootProject / cozyDelegateCommand).value,
        "Effective Cozy runtime command mismatch")
      val cncfsource = selectedCncfSourceRoot.value
      require(cncfsource == (LocalRootProject / selectedCncfSourceRoot).value,
        "Effective CNCF source selection mismatch")
      require(cozyDelegateProjectDir.value == (LocalRootProject / cozyDelegateProjectDir).value,
        "Effective Cozy generator selection mismatch")
      require(CbdSupportProjectYamlBuild.dependencyVersion(fixture, "org.goldenport", "goldenport-cncf") ==
        CbdSupportProjectYamlBuild.dependencyVersion(selected, "org.goldenport", "goldenport-cncf"), "CNCF version mismatch")
      val cncfversion = CbdSupportProjectYamlBuild.dependencyVersion(selected, "org.goldenport", "goldenport-cncf")
      val effectivedependencies = Seq(libraryDependencies.value, (LocalRootProject / libraryDependencies).value)
      cncfsource match {
        case None =>
          effectivedependencies.foreach { dependencies =>
            require(dependencies.find(module => module.organization == "org.goldenport" && module.name == "goldenport-cncf")
              .exists(_.revision == cncfversion), "Effective CNCF dependency mismatch")
          }
        case Some(_) =>
          effectivedependencies.foreach { dependencies =>
            require(!dependencies.exists(module => module.organization == "org.goldenport" && module.name == "goldenport-cncf"),
              "Effective CNCF source dependency must not include an artifact")
          }
      }
      val dependencycheck: Def.Initialize[Task[Unit]] = cncfsource match {
        case None => Def.task { () }
        case Some(directory) => Def.task {
          val observed = (RootProject(directory.toURI) / version).value
          require(observed == cncfversion, "Effective CNCF source version mismatch")
          ()
        }
      }
      require(fixture.boolean("publication.source_manifest.enabled").contains(false) &&
        selected.boolean("publication.source_manifest.enabled").contains(false), "Source manifest must be explicitly disabled")
      val rootconfig = (LocalRootProject / baseDirectory).value / "conf/cozy/config.yaml"
      val fixtureconfig = baseDirectory.value / "conf/cozy/config.yaml"
      Seq(rootconfig, fixtureconfig).foreach { config =>
        require(!Files.isSymbolicLink(config.toPath) && Files.isRegularFile(config.toPath),
          s"Actual publication operation configuration is required: $config")
        require(CbdSupportProjectYamlBuild.load(config).boolean("publication.source_manifest.enabled").contains(false),
          s"Operational source manifest must be explicitly disabled: $config")
      }
      Def.task {
        dependencycheck.value
        val generated = cozyGenerate.value
        require(generated.nonEmpty && generated.forall(_.isFile), "Generated public sources are required")
        val mainjar = (Compile / packageBin).value
        val apijar = cozyComponentApiJar.value.getOrElse(sys.error("Actual public API JAR is required"))
        val scaladoc = cozyScaladocArchive.value
        val car = cozyBuildCar.value
        val sar = cozyBuildSar.value
        val publication = cozyPublishProject.value
        val projectroot = baseDirectory.value
        val targetroot = target.value.toPath.toAbsolutePath.normalize
        val paths = Seq("projectRoot" -> projectroot, "mainJar" -> mainjar, "apiJar" -> apijar,
          "generatedRoot" -> cozyTargetDir.value, "metadataRoot" -> (target.value / "cozy"),
          "scaladocRoot" -> scaladoc, "car" -> car, "sar" -> sar, "publicationRoot" -> publication)
        val properties = new Properties
        properties.setProperty("schema", "cbdsupport.output-exclusion.v1")
        paths.foreach { case (key, output) =>
          val path = output.toPath.toAbsolutePath.normalize
          require(!Files.isSymbolicLink(path), s"Symlink output is not evidence: $key")
          if (Set("mainJar", "apiJar", "car", "sar").contains(key))
            require(Files.isRegularFile(path), s"Missing actual file: $key")
          else require(Files.isDirectory(path), s"Missing actual directory: $key")
          require(key == "projectRoot" || path.startsWith(targetroot), s"Output outside fixture target: $key")
          properties.setProperty(key, path.toString)
        }
        val receipt = target.value / "internal-model-output-exclusion.properties"
        IO.createDirectory(receipt.getParentFile)
        val stream = Files.newOutputStream(receipt.toPath)
        try properties.store(stream, "Actual task path locators only") finally stream.close()
        receipt
      }
    }.value
  )
}
