package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject}
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.Producer

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationJsonPart {
  def _diagnostic(kind: DiagnosticKind, detail: String, path: String): Diagnostic = Diagnostic(kind, detail, None, None, None, path)
  def _malformed(path: String, detail: String): Diagnostic = _diagnostic(DiagnosticKind.Malformed, detail, path)
  def _child(path: String, field: String): String = (if (path == "/") "" else path) + "/" + field.replace("~", "~0").replace("/", "~1")
  def _nonnull[A](value: A, path: String): Either[Diagnostic, A] = Option(value).toRight(_malformed(path, "A nonnull value is required."))
  def _sequence[A](values: Vector[Either[Diagnostic, A]]): Either[Diagnostic, Vector[A]] =
    values.foldLeft[Either[Diagnostic, Vector[A]]](Right(Vector.empty))((acc, next) => for { xs <- acc; x <- next } yield xs :+ x)
  def _context[A](result: Either[Diagnostic, A], source: Option[Source], scope: Option[String] = None, component: Option[ComponentId] = None): Either[Diagnostic, A] =
    result.left.map(e => e.copy(source = e.source.orElse(source), scopeId = e.scopeId.orElse(scope), component = e.component.orElse(component)))
  def _object(json: Json, fields: Vector[String], path: String): Either[Diagnostic, JsonObject] = for {
    obj <- json.asObject.toRight(_malformed(path, "A JSON object is required."))
    _ <- fields.find(!obj.contains(_)).map(x => Left(_malformed(_child(path, x), "A mandatory field is missing."))).getOrElse(Right(()))
    _ <- obj.keys.find(!fields.contains(_)).map(x => Left(_malformed(_child(path, x), "An unknown field is not allowed."))).getOrElse(Right(()))
  } yield obj
  def _read[A](obj: JsonObject, field: String, path: String)(decode: (Json, String) => Either[Diagnostic, A]): Either[Diagnostic, A] =
    obj(field).toRight(_malformed(_child(path, field), "A mandatory field is missing.")).flatMap(decode(_, _child(path, field)))
  def _text(value: String, path: String): Either[Diagnostic, Unit] = {
    def _whitespace_(cp: Int) = Character.isWhitespace(cp) || Character.isSpaceChar(cp)
    Either.cond(value != null && value.nonEmpty && !_whitespace_(value.codePointAt(0)) && !_whitespace_(value.codePointBefore(value.length)) &&
      !value.codePoints().anyMatch(cp => Character.isISOControl(cp)), (), _malformed(path, "Text must be nonempty, unpadded and free of ISO controls."))
  }
  def _path(value: String, path: String): Either[Diagnostic, Unit] = Either.cond(value != null && value.nonEmpty &&
    !value.codePoints().anyMatch(cp => Character.isISOControl(cp)) && !value.startsWith("/") && !value.contains("\\") &&
    !value.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*") && value.split("/", -1).forall(x => x.nonEmpty && x != "." && x != ".."),
    (), _malformed(path, "A canonical project-relative POSIX path is required."))
  def _string(json: Json, path: String): Either[Diagnostic, String] = json.asString.toRight(_malformed(path, "A JSON string is required.")).flatMap(x => _text(x, path).map(_ => x))
  def _integer(json: Json, path: String): Either[Diagnostic, Int] = json.asNumber.filter(_.toString.matches("-?(0|[1-9][0-9]*)")).flatMap(_.toInt).toRight(_malformed(path, "An integer in the supported range is required."))
  def _vector[A](json: Json, path: String)(decode: (Json, String) => Either[Diagnostic, A]): Either[Diagnostic, Vector[A]] =
    json.asArray.toRight(_malformed(path, "A JSON array is required.")).flatMap(xs => _sequence(xs.zipWithIndex.map((x, i) => decode(x, _child(path, i.toString)))))
  def _values[A](values: Vector[A], path: String)(check: (A, String) => Either[Diagnostic, Unit]): Either[Diagnostic, Unit] =
    _nonnull(values, path).flatMap(xs => _sequence(xs.zipWithIndex.map((x, i) => check(x, _child(path, i.toString)))).map(_ => ()))
  def _tag[A](json: Json, path: String, values: Vector[(String, A)]): Either[Diagnostic, A] =
    _string(json, path).flatMap(x => values.find(_._1 == x).map(_._2).toRight(_diagnostic(DiagnosticKind.Unsupported, "Unsupported closed contract tag.", path)))
  def _boundary(json: Json, path: String): Either[Diagnostic, ReferenceBoundary] = _tag(json, path, ReferenceBoundary.values.toVector.map(x => x.tag -> x))
  def _producer(json: Json, path: String): Either[Diagnostic, Producer] = for {
    obj <- _object(json, Vector("id", "version"), path)
    id <- _read(obj, "id", path)(_string)
    version <- _read(obj, "version", path)(_string)
  } yield Producer(id, version)
  def _source(json: Json, path: String): Either[Diagnostic, Source] = for {
    obj <- _object(json, Vector("modelAuthority", "path", "sourceRevision", "line"), path)
    authority <- _read(obj, "modelAuthority", path)(_string)
    name <- _read(obj, "path", path) { (j, p) => j.asString.toRight(_malformed(p, "A path string is required.")).flatMap(x => _path(x, p).map(_ => x)) }
    revision <- _read(obj, "sourceRevision", path)(_string)
    line <- _read(obj, "line", path) { (j, p) => if (j.isNull) Right(None) else _integer(j, p).flatMap(x => Either.cond(x > 0, Some(x), _malformed(p, "A supplied line must be positive."))) }
  } yield Source(authority, name, revision, line)
  def _check_source(value: Source, path: String): Either[Diagnostic, Unit] = _context(for {
    x <- _nonnull(value, path)
    _ <- _text(x.modelAuthority, _child(path, "modelAuthority"))
    _ <- _path(x.path, _child(path, "path"))
    _ <- _text(x.sourceRevision, _child(path, "sourceRevision"))
    line <- _nonnull(x.line, _child(path, "line"))
    _ <- Either.cond(line.forall(_ > 0), (), _malformed(_child(path, "line"), "A supplied line must be positive."))
  } yield (), Option(value))
  def _model_id(json: Json, path: String): Either[Diagnostic, ModelElementId] = for {
    obj <- _object(json, Vector("modelId", "elementId"), path)
    model <- _read(obj, "modelId", path)(_string)
    element <- _read(obj, "elementId", path)(_string)
  } yield ModelElementId(model, element)
  def _check_model_id(value: ModelElementId, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path); _ <- _text(x.modelId, _child(path, "modelId")); _ <- _text(x.elementId, _child(path, "elementId"))
  } yield ()
  def _qualified(json: Json, field: String, path: String): Either[Diagnostic, (String, String)] = for {
    obj <- _object(json, Vector("vocabularyId", field), path)
    vocabulary <- _read(obj, "vocabularyId", path)(_string)
    local <- _read(obj, field, path)(_string)
  } yield (vocabulary, local)
  def _presence[A](json: Json, path: String)(decode: (Json, String) => Either[Diagnostic, A]): Either[Diagnostic, Presence[A]] = for {
    obj <- json.asObject.toRight(_malformed(path, "An explicit Presence object is required."))
    state <- _read(obj, "state", path)((j, p) => _tag(j, p, Vector("PRESENT" -> true, "ABSENT" -> false)))
    value <- if (state) for {
      exact <- _object(json, Vector("state", "value"), path)
      item <- _read(exact, "value", path)(decode)
    } yield Present(item) else for {
      exact <- _object(json, Vector("state", "reason", "detail"), path)
      reason <- _read(exact, "reason", path)((j, p) => _tag(j, p, AbsenceReason.values.toVector.map(x => x.toString -> x)))
      detail <- _read(exact, "detail", path)(_string)
    } yield Absent(reason, detail)
  } yield value
  def _items[A](value: Presence[Vector[A]]): Vector[A] = value match { case Present(xs) => xs; case _: Absent => Vector.empty }
  def _origin(json: Json, path: String): Either[Diagnostic, Origin] = for {
    obj <- json.asObject.toRight(_malformed(path, "An Origin object is required."))
    kind <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("DECLARED" -> true, "DERIVED" -> false)))
    value <- if (kind) _object(json, Vector("kind"), path).map(_ => Declared) else for {
      exact <- _object(json, Vector("kind", "ruleId", "inputs"), path)
      rule <- _read(exact, "ruleId", path)(_string)
      inputs <- _read(exact, "inputs", path)((j, p) => _vector(j, p)(_source))
      _ <- Either.cond(inputs.nonEmpty, (), _malformed(_child(path, "inputs"), "Derived origin requires nonempty inputs."))
    } yield Derived(rule, inputs)
  } yield value
  def _label(json: Json, path: String): Either[Diagnostic, LocalizedLabel] = for {
    obj <- _object(json, Vector("text", "language"), path)
    text <- _read(obj, "text", path)(_string)
    language <- _read(obj, "language", path)((j, p) => if (j.isNull) Right(None) else _string(j, p).map(Some(_)))
  } yield LocalizedLabel(text, language)
  def _authority(source: Source, authority: String, path: String): Either[Diagnostic, Unit] =
    _context(Either.cond(source.modelAuthority == authority, (), _diagnostic(DiagnosticKind.Incompatible, "Source authority differs from its owner.", _child(path, "modelAuthority"))), Some(source))
}
