package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.{ExecutionContext, SubjectKind}

/*
 * @since   Oct. 27, 2026
 * @version Nov.  5, 2026
 */
/** Server-selected principal binding; transport input never constructs a grant. */
object ModelEditAuthorization {
  final case class Binding(principalId: String, canRead: Boolean,
    authorization: Option[ModelEditSession.Authorization])

  /** Shared CNCF principal/session boundary; a domain grant is selected separately. */
  def authenticatedPrincipalC(using ctx: ExecutionContext): Consequence[String] = {
    val security = ctx.security
    val now = ctx.clock.instant()
    val authenticated = security.subjectKind != SubjectKind.Anonymous && security.subjectKind != SubjectKind.Unspecified &&
      security.principal.id.value.trim.nonEmpty && security.session.forall(value =>
        value.authenticatedAt.forall(time => !time.isAfter(now)) && value.expiresAt.forall(now.isBefore))
    if (authenticated) Consequence.success(security.principal.id.value)
    else Consequence.securityPermissionDenied("An authenticated principal with an applicable session lifetime is required")
  }

  def authorize(bindings: Vector[Binding], session: ModelEditSession,
    editing: Boolean)(using ctx: ExecutionContext): Consequence[Binding] =
    authenticatedPrincipalC.flatMap { principal =>
      bindings.filter(_.principalId == principal) match {
        case Vector(binding) if binding.canRead && (!editing || binding.authorization.exists(value =>
          value.session == session.reference && value.scope == session.scope)) => Consequence.success(binding)
        case _ => Consequence.securityPermissionDenied("Model Edit requires an authenticated exact server principal binding and applicable access")
      }
    }
}
