package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ComponentDashboardWebEntryAuthorizationDecision

final case class ComponentDashboardWebEntryAuthorizationAffirmative(
  component: ComponentDashboardComponentIdentity
) extends ComponentDashboardWebEntryAuthorizationDecision

case object ComponentDashboardWebEntryAuthorizationMissing extends ComponentDashboardWebEntryAuthorizationDecision

case object ComponentDashboardWebEntryAuthorizationDenied extends ComponentDashboardWebEntryAuthorizationDecision

case object ComponentDashboardWebEntryAuthorizationExpired extends ComponentDashboardWebEntryAuthorizationDecision

case object ComponentDashboardWebEntryAuthorizationAmbiguous extends ComponentDashboardWebEntryAuthorizationDecision

final case class ComponentDashboardWebEntryFailure(violations: Vector[String])

final case class ComponentDashboardWebEntry(dashboard: ComponentDashboard)

object ComponentDashboardWebEntry {
  def create(
    decision: ComponentDashboardWebEntryAuthorizationDecision,
    dashboard: ComponentDashboard
  ): Either[ComponentDashboardWebEntryFailure, ComponentDashboardWebEntry] =
    decision match {
      case ComponentDashboardWebEntryAuthorizationAffirmative(component) =>
        if (component.value.trim.isEmpty)
          Left(ComponentDashboardWebEntryFailure(Vector("Component Dashboard Web entry direct Component identity must not be blank.")))
        else if (dashboard.component.value.trim.isEmpty)
          Left(ComponentDashboardWebEntryFailure(Vector("Component Dashboard Web entry Dashboard Component identity must not be blank.")))
        else if (component != dashboard.component)
          Left(
            ComponentDashboardWebEntryFailure(
              Vector(s"Component Dashboard Web entry direct Component '${component.value}' does not match Dashboard Component '${dashboard.component.value}'.")
            )
          )
        else
          Right(ComponentDashboardWebEntry(dashboard))
      case _ =>
        Left(
          ComponentDashboardWebEntryFailure(
            Vector("Component Dashboard Web entry authorization decision must affirm one direct Component identity.")
          )
        )
    }
}
