import sbt._
import sbt.Keys._
import org.goldenport.cozy.CozyPlugin.autoImport._

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Selection metadata only. Actual subprocesses belong to the explicit producer-focused Spec. */
object ModelEditProducerIntegrationBuild {
  val selectedModelEditProducerRoot = settingKey[Option[File]]("Explicit independently delivered model-edit producer")
  val settings: Seq[Def.Setting[_]] = Seq(
    selectedModelEditProducerRoot := None,
    Test / javaOptions ++= Vector("textus.cbd.modelEditE2eRoot", "textus.cbd.modelEditE2ePreparedRoot")
      .flatMap(key => sys.props.get(key).map(value => "-D" + key + "=" + value)),
    Test / javaOptions ++= sys.env.get("TEXTUS_CBD_MODEL_EDIT_COZY_EXECUTABLE")
      .toVector.map(value => "-Dtextus.cbd.modelEditCozyExecutable=" + value),
    Test / javaOptions ++= selectedModelEditProducerRoot.value.toSeq.flatMap { directory =>
      Seq("-Dtextus.cbd.modelEditProducerRoot=" + directory.getCanonicalPath,
        "-Dtextus.cbd.modelEditProducerVersion=" + CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.modelEditProducerVersion"),
        "-Dtextus.cbd.modelEditProducerRevision=" + CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.modelEditProducerRevision"))
    }
  )
}
