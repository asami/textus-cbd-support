import org.goldenport.cozy.CozyPlugin.autoImport._
import sbt.Keys.*

lazy val root = project
  .in(file("."))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .settings(
    organization := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.organization"),
    name := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.name"),
    version := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.component.version"),
    scalaVersion := CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.scalaVersion"),
    useCoursier := false,

    resolvers += Resolver.defaultLocal,
    resolvers += Resolver.file("Local Ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    resolvers += "Local Maven Repository" at ("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven",
    libraryDependencies ++= CbdSupportProjectYamlBuild.dependencies(cozyProjectMetadata.value),

    cozyGeneratorBackend := "cozy",
    cozyDelegateProjectDir := None,
    cozyDelegateCommand := Seq("cozy"),
    cozyManifestMetadata ++=
      cozyProjectMetadata.value.mapUnder("packaging.car.manifest_metadata") ++
        Map("component" -> CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.component.name"))
  )
