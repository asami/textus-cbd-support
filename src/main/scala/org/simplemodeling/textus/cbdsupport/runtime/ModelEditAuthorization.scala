package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.{ExecutionContext, SubjectKind}

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Server-selected principal binding; transport input never constructs a grant. */
object ModelEditAuthorization {
  final case class Binding(principalId: String, canRead: Boolean,
    authorization: Option[ModelEditSession.Authorization])

  def authorize(bindings: Vector[Binding], session: ModelEditSession,
    editing: Boolean)(using ctx: ExecutionContext): Consequence[Binding] = {
    val security = ctx.security
    val now = ctx.clock.instant()
    val authenticated = security.subjectKind != SubjectKind.Anonymous && security.subjectKind != SubjectKind.Unspecified &&
      security.principal.id.value.trim.nonEmpty && security.session.forall(value =>
        value.authenticatedAt.forall(time => !time.isAfter(now)) && value.expiresAt.forall(now.isBefore))
    val selected = bindings.filter(_.principalId == security.principal.id.value)
    selected match {
      case Vector(binding) if authenticated && binding.canRead && (!editing || binding.authorization.exists(value =>
        value.session == session.reference && value.scope == session.scope)) => Consequence.success(binding)
      case _ => Consequence.securityPermissionDenied("Model Edit requires an authenticated exact server principal binding and applicable access")
    }
  }
}
