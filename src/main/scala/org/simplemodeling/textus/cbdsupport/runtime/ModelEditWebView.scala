package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Read-only rendering for the host's contextual Palette and candidate confirmation. */
object ModelEditWebView {
  def palette(view: ModelEditPalette.View): String =
    "<section class=\"model-edit-palette\" data-candidate=\"" + _escape(_reference(view.candidate)) +
      "\"><h2>Update Palette</h2><p>" + _escape(view.context.surface.toString) + " — " + _escape(view.context.subject.toString) +
      "</p><ul>" + view.entries.map(value => "<li><button type=\"button\" data-command=\"" + _escape(value.kind.toString) +
        "\">" + _escape(value.label) + "</button><span>" + value.inputs.map(_escape).mkString(", ") + "</span></li>").mkString +
      "</ul><p>Changes remain in the candidate until separate review and approval.</p></section>"

  def confirmation(view: ModelEditViewProjection.View): String = {
    val selected = view.confirmation
    val state = if (selected.abandoned) "Abandoned candidate" else selected.state match {
      case ModelEditViewProjection.State.CanonicalBasis => "Canonical basis"
      case ModelEditViewProjection.State.Candidate => "Candidate changes"
    }
    "<section class=\"model-edit-confirmation\"><h2>" + state + "</h2><p>Candidate " +
      _escape(_reference(selected.candidate)) + "</p><p>Canonical source " + _escape(selected.canonicalSource.identity) + " @ " +
      _escape(selected.canonicalSource.revision) + "</p><ol>" + selected.operations.map(value => "<li>" + _escape(_reference(value)) + "</li>").mkString +
      "</ol><h3>Actor goals</h3><ul>" + view.actorGoals.goals.map { goal =>
        "<li>" + _escape(goal.identity.value) + ": " + _escape(goal.intent) + "; Actors: " +
          goal.actors.map(value => _escape(value.value)).mkString(", ") + "; Use Cases: " +
          goal.useCases.map(value => _escape(value.value)).mkString(", ") + "</li>"
      }.mkString + "</ul><h3>Development traces</h3><ul>" + view.traces.map { trace =>
        "<li>Use Case " + _escape(trace.useCaseIdentity.value) + "<ul>" + trace.slices.map { slice =>
          "<li>Slice " + _escape(slice.identity.value) + ": " + _escape(slice.title) + " — " + _escape(slice.intent) +
            " (" + _escape(slice.lifecycle.toString) + ")<ul>" + slice.fields.map(field => "<li>" + _escape(field.name) +
              ": " + _escape(field.datatype) + " " + _escape(field.multiplicity) + "</li>").mkString +
            trace.specificationLinks.filter(_.sliceIdentity == slice.identity).map(edge => "<li>Specification " +
              _escape(edge.specification.toString) + " (" + _escape(_reference(edge.reference)) + ")</li>").mkString +
            trace.ticketLinks.filter(_.sliceIdentity == slice.identity).map(edge => "<li>Ticket reference " +
              _escape(edge.ticket.toString) + " (" + _escape(_reference(edge.reference)) + ")</li>").mkString + "</ul></li>"
        }.mkString + "</ul></li>"
      }.mkString + "</ul><p>Confirmation grants no canonical write or external ticket mutation.</p></section>"
  }

  private def _reference(reference: InternalModelRecordReference): String = reference.recordId.value + " @ " + reference.recordRevision.value
  private def _escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;").replace("'", "&#39;")
}
