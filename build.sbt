import org.goldenport.cozy.CozyPlugin.autoImport._
import org.goldenport.cozy.CozyProjectIdentityEvidence
import sbt.Keys.*

lazy val projectIdentityEvidence = settingKey[CozyProjectIdentityEvidence]("Admitted project.yaml component identity evidence")

// Explicit development source mode; an invalid selection never resolves the artifact instead.
lazy val cncfSourceRoot = sys.props.get("textus.cbd.cncfSourceRoot")
  .orElse(sys.env.get("TEXTUS_CBD_SUPPORT_CNCF_SOURCE_ROOT"))
  .map { selected =>
    require(selected.trim.nonEmpty, "textus.cbd.cncfSourceRoot requires an explicit source directory")
    val directory = file(selected).getCanonicalFile
    require(directory.isDirectory && java.nio.file.Files.isRegularFile(
      directory.toPath.resolve("build.sbt"), java.nio.file.LinkOption.NOFOLLOW_LINKS),
      "textus.cbd.cncfSourceRoot requires a canonical CNCF source directory with build.sbt")
    directory
  }

lazy val cozyGeneratorRoot = sys.props.get("textus.cbd.cozyGeneratorRoot")
  .orElse(sys.props.get("cncf.cozyGeneratorRoot"))
  .map { selected =>
    require(selected.trim.nonEmpty, "textus.cbd.cozyGeneratorRoot requires an explicit generator directory")
    val directory = file(selected).getCanonicalFile
    require(directory.isDirectory && java.nio.file.Files.isRegularFile(
      directory.toPath.resolve("build.sbt"), java.nio.file.LinkOption.NOFOLLOW_LINKS),
      "textus.cbd.cozyGeneratorRoot requires a canonical Cozy generator directory with build.sbt")
    directory
  }

// Source-mode service generation and producer acceptance share the selected Cozy input.
lazy val capabilityProducerRoot = sys.props.get("textus.cbd.capabilityProducerRoot")
  .map { selected =>
    require(selected.trim.nonEmpty, "textus.cbd.capabilityProducerRoot requires an explicit producer directory")
    require(cncfSourceRoot.nonEmpty, "Capability producer integration requires textus.cbd.cncfSourceRoot")
    val directory = file(selected).getCanonicalFile
    require(directory.isDirectory && java.nio.file.Files.isRegularFile(
      directory.toPath.resolve("build.sbt"), java.nio.file.LinkOption.NOFOLLOW_LINKS),
      "textus.cbd.capabilityProducerRoot requires a canonical Cozy source directory with build.sbt")
    directory
  }.orElse(if (cncfSourceRoot.nonEmpty) cozyGeneratorRoot else None)

lazy val root = project
  .in(file("."))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .dependsOn(cncfSourceRoot.toSeq.map(directory => RootProject(directory.toURI) % "compile->compile;test->test"): _*)
  .settings(
    projectIdentityEvidence := CbdSupportProjectYamlBuild.admitted(cozyProjectMetadata.value, scalaBinaryVersion.value),
    organization := CbdSupportProjectYamlBuild.organization(projectIdentityEvidence.value),
    moduleName := CbdSupportProjectYamlBuild.moduleName(projectIdentityEvidence.value),
    name := moduleName.value,
    version := CbdSupportProjectYamlBuild.version(projectIdentityEvidence.value),
    scalaVersion := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.scalaVersion"),
    useCoursier := false,

    resolvers += Resolver.defaultLocal,
    resolvers += Resolver.file("Local Ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    resolvers += "Local Maven Repository" at ("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven",
    libraryDependencies ++= CbdSupportProjectYamlBuild.dependencies(cozyProjectMetadata.value).filterNot { dependency =>
      cncfSourceRoot.nonEmpty && dependency.organization == "org.goldenport" && dependency.name == "goldenport-cncf"
    },
    Test / unmanagedSourceDirectories ++= cncfSourceRoot.toSeq.map(_ => baseDirectory.value / "src/test-current-cncf/scala"),

    cozyGeneratorBackend := "cozy",
    cozyDelegateProjectDir := cozyGeneratorRoot,
    cozyDelegateCommand := Seq(
      "cozy",
      "--runtime",
      CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.cozyVersion")
    ),
    cozyCarName := CbdSupportProjectYamlBuild.carBaseName(projectIdentityEvidence.value),
    cozyManifestMetadata ++=
      cozyProjectMetadata.value.mapUnder("packaging.car.manifest_metadata") ++
        CbdSupportProjectYamlBuild.manifestMetadata(projectIdentityEvidence.value),
    Test / resourceGenerators += Def.task {
      val receipt = (internalModelExclusionFixture / InternalModelExclusionBuild.prepareInternalModelExclusionEvidence).value
      val resource = (Test / resourceManaged).value / "internal-model-output-exclusion.properties"
      IO.copyFile(receipt, resource)
      Seq(resource)
    }.taskValue
  )
  .settings(InternalModelExclusionBuild.sourceDependencySettings)
  .settings(InternalModelExclusionBuild.selectedCncfSourceRoot := cncfSourceRoot)
  .settings(CapabilityProducerIntegrationBuild.settings)
  .settings(CapabilityProducerIntegrationBuild.selectedProducerRoot := capabilityProducerRoot)

lazy val internalModelExclusionFixture = project
  .in(file("src/test/fixtures/internal-model-output-exclusion"))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .dependsOn(cncfSourceRoot.toSeq.map(directory => RootProject(directory.toURI) % "compile->compile;test->test"): _*)
  .settings(InternalModelExclusionBuild.settings)
  .settings(InternalModelExclusionBuild.sourceDependencySettings)
  .settings(InternalModelExclusionBuild.selectedCncfSourceRoot := cncfSourceRoot)
