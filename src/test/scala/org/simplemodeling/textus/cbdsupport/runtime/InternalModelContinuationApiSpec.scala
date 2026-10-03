package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant
import org.goldenport.{Consequence, Conclusion}
import org.goldenport.observation.Taxonomy
import org.goldenport.cncf.component.{Component, ComponentInstanceId}
import org.goldenport.cncf.context.{Capability, ExecutionContext, Principal, PrincipalId, SecurityLevel, SessionContext, SubjectKind}
import org.goldenport.protocol.{Protocol, Request}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent

/**
 * Authenticated internal operations and package-only continuation; authored, not validated.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  4, 2026
 */
final class InternalModelContinuationApiSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelRetainedHistoryFixture.{value as fixtureValue, *}
  private val _operations = Vector("read", "resume", "propose", "review", "record", "expire", "delete")
  private val _raw_sentinel = "RAW-PROMPT-RESPONSE-INTERNAL-API-SENTINEL"
  private val _exclusions = Map(
    "entity" -> Vector("createInternalModelHistoryEntry", "createInternalModelHistoryEntryRecord",
      "loadInternalModelHistoryEntry", "loadInternalModelHistoryEntryRecord", "saveInternalModelHistoryEntry",
      "saveInternalModelHistoryEntryRecord", "updateInternalModelHistoryEntry", "updateInternalModelHistoryEntryRecord",
      "deleteInternalModelHistoryEntry", "deleteInternalModelHistoryEntryHard", "searchInternalModelHistoryEntry",
      "searchInternalModelHistoryEntryRecord"),
    "aggregate" -> Vector("createInternalModelHistoryEntry", "loadInternalModelHistoryEntry", "saveInternalModelHistoryEntry",
      "updateInternalModelHistoryEntry", "deleteInternalModelHistoryEntry", "searchInternalModelHistoryEntry"),
    "view" -> Vector("loadInternalModelHistoryEntry", "loadInternalModelHistoryEntryByView", "searchInternalModelHistoryEntry",
      "searchInternalModelHistoryEntryRecord", "loadInternalModelHistoryEntrySummary", "searchInternalModelHistoryEntrySummary",
      "searchInternalModelHistoryEntrySummaryRecord", "loadInternalModelHistoryEntryDetail", "searchInternalModelHistoryEntryDetail",
      "searchInternalModelHistoryEntryDetailRecord"))

  "Authenticated internal continuation API" should {
    "the closed trusted-context operation matrix" which {
      "S1 admit every role-operation pair exactly and deny all unrelated wildcard roles" in {
        Given("the four admitted roles and unrelated system, internal, user, content-manager and provider roles")
        withCapture { (root, capture, request) => withDatabase { path =>
          val db = database(path)
          val before = InternalModelContinuationFixture.treeBytes(root)
          try {
            val roles = Vector("viewer", "reviewer", "operator", "admin", "system", "internal", "user", "content_admin", "provider")
            roles.foreach { role => _operations.foreach { operation =>
              val probe = new StoreProbe()
              val context = roleContext(db.context, Set(role))
              val adapter = api(actionCore(context, Some(db.component)), capture, Some(probe))
              When("an authenticated principal requests " + operation + " with only role " + role)
              val result = _invoke(adapter, operation, request)
              Then("only the exact matrix pair is admitted before any provider or source action")
              if (_allowed(operation).contains(role)) {
                result.isSuccess shouldBe true
                probe.calls shouldBe (if (operation == "resume") Vector.empty else Vector(if (Set("propose", "review", "record").contains(operation)) "record" else operation))
              } else {
                _denied(result)
                probe.calls shouldBe empty
              }
              db.providers.calls shouldBe 0
              db.sources.calls shouldBe 0
            }}
            InternalModelContinuationFixture.treeBytes(root) shouldBe before
          } finally db.close()
        }}
      }

      "S2 reuse admitted attribute capability and level vocabulary with stable actor priority" in {
        Given("role authority supplied through each existing framework vocabulary channel")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val baseline = roleContext(db.context, Set("viewer"), SubjectKind.Service)
            val contexts = Vector(
              ExecutionContext.withSecurityContext(baseline, baseline.security.copy(capabilities = Set.empty, level = SecurityLevel("user"))),
              ExecutionContext.withSecurityContext(baseline, baseline.security.copy(principal = SpecPrincipal(PrincipalId(principalId), Map.empty),
                capabilities = Set(Capability("viewer")), level = SecurityLevel("user"))),
              ExecutionContext.withSecurityContext(baseline, baseline.security.copy(principal = SpecPrincipal(PrincipalId(principalId), Map.empty),
                capabilities = Set.empty, level = SecurityLevel("viewer"))))
            When("read authority is supplied through attributes, capabilities or level")
            val results = contexts.map(context => api(actionCore(context), capture, None).resume(request))
            val probe = new StoreProbe()
            val prioritized = api(actionCore(roleContext(db.context, Set("viewer", "reviewer", "operator", "admin"), SubjectKind.Service)), capture, Some(probe))
            val recorded = prioritized.record(input())
            Then("the vocabulary remains admitted and the highest stable role supplies actor attribution")
            results.forall(_.isSuccess) shouldBe true
            recorded.toOption shouldBe Some(InternalModelHistoryResult.Unavailable)
            _record(probe.documents.head).actor shouldBe InternalModelDecisionActor("service", principalId, "admin")
          } finally db.close()
        }}
      }

      "S3 reject anonymous unspecified null and malformed security graphs without effects or raw diagnostics" in {
        Given("every malformed trusted security dimension and a raw request sentinel")
        withCapture { (root, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val before = InternalModelContinuationFixture.treeBytes(root)
            val good = roleContext(db.context, Set("admin"))
            val security = good.security
            val invalid = Vector(
              null, ExecutionContext.withSecurityContext(good, null),
              ExecutionContext.withSecurityContext(good, security.copy(subjectKind = SubjectKind.Anonymous)),
              ExecutionContext.withSecurityContext(good, security.copy(subjectKind = SubjectKind.Unspecified)),
              ExecutionContext.withSecurityContext(good, security.copy(subjectKind = null)),
              ExecutionContext.withSecurityContext(good, security.copy(principal = null)),
              ExecutionContext.withSecurityContext(good, security.copy(principal = SpecPrincipal(null, Map.empty))),
              ExecutionContext.withSecurityContext(good, security.copy(principal = SpecPrincipal(PrincipalId(" "), Map.empty))),
              ExecutionContext.withSecurityContext(good, security.copy(principal = SpecPrincipal(PrincipalId(principalId), null))),
              ExecutionContext.withSecurityContext(good, security.copy(principal = SpecPrincipal(PrincipalId(principalId), Map(null.asInstanceOf[String] -> "admin")))),
              ExecutionContext.withSecurityContext(good, security.copy(principal = SpecPrincipal(PrincipalId(principalId), Map("role" -> null)))),
              ExecutionContext.withSecurityContext(good, security.copy(capabilities = null)),
              ExecutionContext.withSecurityContext(good, security.copy(capabilities = Set(null.asInstanceOf[Capability]))),
              ExecutionContext.withSecurityContext(good, security.copy(capabilities = Set(Capability(null)))),
              ExecutionContext.withSecurityContext(good, security.copy(level = null)),
              ExecutionContext.withSecurityContext(good, security.copy(level = SecurityLevel(null))),
              ExecutionContext.withSecurityContext(good, security.copy(session = null)))
            invalid.foreach { context =>
              val probe = new StoreProbe()
              val adapter = api(actionCore(context), capture, Some(probe))
              When("all operations encounter one malformed security graph")
              val results = _operations.map(operation => _invoke(adapter, operation, request, raw = true))
              Then("generic security denial precedes storage, providers and source access")
              results.foreach(_denied)
              results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
              probe.calls shouldBe empty
              probe.documents shouldBe empty
            }
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
            InternalModelContinuationFixture.treeBytes(root) shouldBe before
          } finally db.close()
        }}
      }

      "S4 reject principal mismatch expired sessions future authentication and malformed sessions" in {
        Given("exact server principal binding and session time/null graph violations")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val base = roleContext(db.context, Set("admin"), SubjectKind.Service)
            val sessions = Vector(Some(SessionContext(expiresAt = Some(time))),
              Some(SessionContext(expiresAt = Some(time.minusNanos(1L)))),
              Some(SessionContext(authenticatedAt = Some(time.plusNanos(1L)))),
              Some(null.asInstanceOf[SessionContext]), Some(SessionContext(authenticatedAt = null)),
              Some(SessionContext(expiresAt = Some(null.asInstanceOf[Instant]))),
              Some(SessionContext(attributes = null)), Some(SessionContext(tokenId = Some(null.asInstanceOf[String]))))
            val contexts = sessions.map(session => roleContext(base, Set("admin"), SubjectKind.Service, session = session)) ++
              Vector(roleContext(base, Set("admin"), identity = "other-principal"),
                roleContext(base, Set("admin"), identity = " " + principalId),
                roleContext(base, Set("admin"), identity = _raw_sentinel))
            contexts.foreach { context =>
              val probe = new StoreProbe()
              val adapter = api(actionCore(context), capture, Some(probe))
              When("the per-call exact principal and session constraints are evaluated")
              val results = _operations.map(operation => _invoke(adapter, operation, request, raw = true))
              Then("every operation rejects before touching storage and never echoes rejected identity/body")
              results.foreach(_denied)
              results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
              probe.calls shouldBe empty
            }
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
          } finally db.close()
        }}
      }

      "S5 admit authenticated service and subsystem principals without an optional session" in {
        Given("server-bound framework service/subsystem kinds and explicit admitted roles")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            Vector(SubjectKind.Service -> "service", SubjectKind.Subsystem -> "subsystem").foreach { case (kind, token) =>
              val probe = new StoreProbe()
              val adapter = api(actionCore(roleContext(db.context, Set("reviewer"), kind)), capture, Some(probe))
              When("that already authenticated framework principal records history and resumes")
              val recorded = adapter.record(input())
              val resumed = adapter.resume(request)
              Then("no session is fabricated and actor kind remains the stable server token")
              recorded.isSuccess shouldBe true
              resumed.isSuccess shouldBe true
              _record(probe.documents.head).actor shouldBe InternalModelDecisionActor(token, principalId, "reviewer")
              probe.calls shouldBe Vector("record")
            }
          } finally db.close()
        }}
      }

      "S6 reject cross-package scope and malformed selections before storage access" in {
        Given("an authorized principal and independently supplied wrong full selection dimensions")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val adapter = api(db.core, capture, Some(probe))
            val selections = Vector(null, selection().copy(reference = null),
              selection().copy(storageId = _raw_sentinel.asInstanceOf[InternalModelHistoryStorageId]),
              selection().copy(packageReference = packageReference.copy(packageId = InternalModelPackageId.from("ffffffff-ffff-ffff-ffff-ffffffffffff").toOption.get)),
              selection().copy(packageReference = packageReference.copy(projectNamespace = InternalModelProjectToken.from("org.other").toOption.get)),
              selection().copy(packageReference = packageReference.copy(projectId = InternalModelProjectToken.from("other").toOption.get)),
              selection().copy(scope = scope.copy(componentIdentity = "other")),
              selection().copy(scope = scope.copy(projectionContextIdentity = "other")),
              selection().copy(scope = scope.copy(selectedUseCaseElementIdentity = "other")))
            When("each exact selector is read, expired or deleted")
            val results = selections.flatMap(selection => Vector(adapter.read(selection), adapter.expire(selection), adapter.delete(selection)))
            Then("wrong binding never becomes a store query and rejected data is absent from diagnostics")
            results.foreach(_invalid)
            results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
            probe.calls shouldBe empty
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
          } finally db.close()
        }}
      }

      "S7 validate actual capture and independent subject references at factory admission" in {
        Given("a captured package and independently selected subject with wrong package scope revision or full artifact reference")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val subjects = Vector(null, subject.copy(subjectId = null.asInstanceOf[InternalModelRecordId]),
              subject.copy(subjectRevision = 0L.asInstanceOf[InternalModelRecordRevision]),
              subject.copy(packageReference = packageReference.copy(projectId = InternalModelProjectToken.from("other").toOption.get)),
              subject.copy(scope = scope.copy(componentIdentity = "other")),
              subject.copy(artifacts = Vector(projection.copy(artifactRevision = InternalModelArtifactRevision.from(999L).toOption.get))),
              subject.copy(artifacts = Vector(projection.copy(role = InternalModelArtifactRole.Approval))),
              subject.copy(artifacts = Vector(InternalModelContinuationFixture.reference("missing", 23L, InternalModelArtifactRole.Projection))))
            val captures = Vector(null, capture.copy(packageContext = capture.packageContext.copy(revision = 32L)),
              capture.copy(artifacts = null), capture.copy(continuityPackage = null))
            When("the trusted owner asks the factory to bind each malformed or contradictory configuration")
            val results = subjects.map(subject => InternalModelContinuationApi.create(db.core, capture, subject, principalId,
              Set(redactionReference), Some(probe))) ++ captures.map(capture => InternalModelContinuationApi.create(db.core,
              capture, subject, principalId, Set(redactionReference), Some(probe))) ++ Vector(
              InternalModelContinuationApi.create(null, capture, subject, principalId, Set(redactionReference), Some(probe)),
              InternalModelContinuationApi.create(db.core, capture, subject, " ", Set(redactionReference), Some(probe)),
              InternalModelContinuationApi.create(db.core, capture, subject, principalId, null, Some(probe)),
              InternalModelContinuationApi.create(db.core, capture, subject, principalId, Set(null.asInstanceOf[InternalModelRecordReference]), Some(probe)),
              InternalModelContinuationApi.create(db.core, capture, subject, principalId, Set(redactionReference), null))
            Then("configuration admission requires actual verified capture and exact independent references without storage or completeness inference")
            results.foreach(_invalid)
            probe.calls shouldBe empty
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
          } finally db.close()
        }}
      }

      "S8 recheck trusted session expiry on every call after factory creation" in {
        Given("an admitted service context whose session expires five seconds after server now")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val context = roleContext(db.context, Set("admin"), SubjectKind.Service,
              session = Some(SessionContext(authenticatedAt = Some(time), expiresAt = Some(time.plusSeconds(5L)))))
            val adapter = api(actionCore(context), capture, Some(probe))
            fixtureValue(adapter.read(selection()))
            When("trusted context time reaches expiry after the adapter has already been created")
            db.clock.set(time.plusSeconds(5L))
            val results = _operations.map(operation => _invoke(adapter, operation, request))
            Then("factory success is never a cached grant and no further store calls occur")
            results.foreach(_denied)
            probe.calls shouldBe Vector("read")
          } finally db.close()
        }}
      }

      "S9 require explicit admitted roles beyond the default test-user privilege" in {
        Given("ExecutionContext.create's default user and a separate context with an explicit viewer role")
        withCapture { (_, capture, request) =>
          val defaultcontext = ExecutionContext.create(ClockForSpec(time))
          val boundid = defaultcontext.security.principal.id.value
          val ungranted = fixtureValue(InternalModelContinuationApi.create(actionCore(defaultcontext), capture, subject,
            boundid, Set(redactionReference), None))
          val admittedcontext = roleContext(defaultcontext, Set("viewer"), identity = boundid)
          val granted = fixtureValue(InternalModelContinuationApi.create(actionCore(admittedcontext), capture, subject,
            boundid, Set(redactionReference), None))
          When("both contexts request the same independently supplied continuation")
          val denied = ungranted.resume(request)
          val accepted = granted.resume(request)
          Then("default test privilege grants no continuation role but explicit admitted viewer does")
          _denied(denied)
          accepted.isSuccess shouldBe true
        }
      }
    }

    "closed record inputs and evidence admission" which {
      "A1 derive actor subject and time on every record-producing route" in {
        forAll(Gen.alphaNumStr.suchThat(_.nonEmpty), Gen.choose(1L, 99999L)) { (suffix, revision) =>
          Given("independent logical record metadata with no request actor, subject or time")
          withCapture { (_, capture, _) => withDatabase { path =>
            val db = database(path, time, Set("reviewer"))
            try {
              val probe = new StoreProbe()
              val adapter = api(db.core, capture, Some(probe))
              val proposal = input(1L, InternalModelRetainedHistoryPayload.Proposal(reference("candidate", 11L), projection))
                .copy(reference = reference("proposal " + suffix, revision))
              val review = input(2L, payloads.head).copy(reference = reference("review " + suffix, revision))
              val alternative = input(3L, InternalModelRetainedHistoryPayload.Alternative(reference("proposal", 23L),
                reference("alternative", 31L), InternalModelRetainedAlternativeDisposition.SelectedAsProposal))
              val evidenceinput = input(4L)
              When("propose, review and both nonsupersession record families cross the server adapter")
              val results = Vector(adapter.propose(proposal), adapter.review(review), adapter.record(alternative), adapter.record(evidenceinput))
              Then("all accounts derive exact independent server subject, kind/identity/role and trusted clock time")
              results.forall(_.isSuccess) shouldBe true
              probe.calls shouldBe Vector.fill(4)("record")
              probe.documents.map(_record).foreach { record =>
                record.subject shouldBe subject
                record.actor shouldBe InternalModelDecisionActor("service", principalId, "reviewer")
                record.occurredAt shouldBe time
              }
              probe.documents.foreach { document =>
                document.selection.storageId shouldBe Vector(proposal, review, alternative, evidenceinput)
                  .find(_.reference == document.selection.reference).get.storageId
                document.content match {
                  case InternalModelHistoryDocumentState.Retained(_, retainedat) => retainedat shouldBe time
                  case _ => fail("Expected retained document")
                }
              }
            } finally db.close()
          }}
        }
      }

      "A2 persist only policy-retained evidence and exact admitted narrative without raw sentinels" in {
        Given("raw prompt/response, mislabeled prompt/response, identities, narrative, unavailable and opaque external references")
        withCapture { (root, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val before = InternalModelContinuationFixture.treeBytes(root)
            val entries = Vector(
              InternalModelEvidenceInput(reference("raw prompt", 7L), InternalModelEvidenceKind.Prompt, InternalModelEvidenceInputPayload.Raw(_raw_sentinel)),
              InternalModelEvidenceInput(reference("raw response", 11L), InternalModelEvidenceKind.Response, InternalModelEvidenceInputPayload.Raw(_raw_sentinel)),
              InternalModelEvidenceInput(reference("safe label prompt", 13L), InternalModelEvidenceKind.Prompt, InternalModelEvidenceInputPayload.SafeIdentity("prompt:metadata")),
              InternalModelEvidenceInput(reference("redacted label response", 17L), InternalModelEvidenceKind.Response, InternalModelEvidenceInputPayload.RedactedText(_raw_sentinel, redactionReference)),
              InternalModelEvidenceInput(reference("provider metadata", 19L), InternalModelEvidenceKind.ProviderIdentity, InternalModelEvidenceInputPayload.SafeIdentity("provider:exact")),
              InternalModelEvidenceInput(reference("model metadata", 23L), InternalModelEvidenceKind.ModelIdentity, InternalModelEvidenceInputPayload.SafeIdentity("model.v2")),
              InternalModelEvidenceInput(reference("tool metadata", 29L), InternalModelEvidenceKind.ToolIdentity, InternalModelEvidenceInputPayload.SafeIdentity("tool:exact")),
              InternalModelEvidenceInput(reference("safe narrative", 31L), InternalModelEvidenceKind.Narrative,
                InternalModelEvidenceInputPayload.RedactedText("  秘匿済み λ e\u0301🧭\n  ", redactionReference)),
              InternalModelEvidenceInput(reference("calltree opaque", 37L), InternalModelEvidenceKind.CallTree, InternalModelEvidenceInputPayload.Raw(_raw_sentinel)),
              InternalModelEvidenceInput(reference("external opaque", 41L), InternalModelEvidenceKind.ExternalEvidence, InternalModelEvidenceInputPayload.Unavailable))
            val adapter = api(db.core, capture, Some(db.store))
            When("the authenticated record route retains evidence before real Entity persistence")
            val result = adapter.record(input(entries = entries))
            val retained = _retained(fixtureValue(result))
            val reread = adapter.read(retained._1)
            val storedtext = db.entity(retained._1).get.history_document.value.value
            Then("raw data never enters result or persisted document while admitted evidence stays exact and unreconstructed")
            retained._2.evidence shouldBe entries.map(entry => fixtureValue(InternalModelEvidencePolicy.retain(entry)))
            reread.toOption shouldBe result.toOption
            storedtext should not include (_raw_sentinel)
            result.toString should not include (_raw_sentinel)
            storedtext should include ("provider:exact")
            storedtext should include ("Unavailable")
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
            InternalModelContinuationFixture.treeBytes(root) shouldBe before
          } finally db.close()
        }}
      }

      "A3 reject unadmitted or forged narrative redaction references before persistence" in {
        Given("redacted narrative labels with a wrong redaction ID or independent revision")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val adapter = api(db.core, capture, Some(probe))
            val references = Vector(reference("unadmitted", 89L), redactionReference.copy(recordRevision = reference("different revision", 90L).recordRevision))
            When("a caller labels narrative as redacted without the exact server-admitted reference")
            val results = references.map(redaction => adapter.record(input(entries = Vector(InternalModelEvidenceInput(reference("narrative", 31L),
              InternalModelEvidenceKind.Narrative, InternalModelEvidenceInputPayload.RedactedText(_raw_sentinel, redaction))))))
            Then("the label supplies no redaction authority and diagnostics omit its text")
            results.foreach(_invalid)
            results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
            probe.calls shouldBe empty
            probe.documents shouldBe empty
          } finally db.close()
        }}
      }

      "A4 reject every wrong route kind null graph and malformed evidence before Entity work" in {
        Given("all payload families and malformed request/evidence/reference graphs")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val adapter = api(db.core, capture, Some(probe))
            val wrongroutes = payloads.flatMap { payload =>
              val value = input(payload = payload)
              Vector(
                Option.when(payload.kind != InternalModelRetainedHistoryKind.Proposal)(() => adapter.propose(value)),
                Option.when(payload.kind != InternalModelRetainedHistoryKind.Review)(() => adapter.review(value)),
                Option.when(Set(InternalModelRetainedHistoryKind.Review, InternalModelRetainedHistoryKind.Proposal).contains(payload.kind))(() => adapter.record(value))).flatten
            }
            val malformed = Vector(null, input().copy(payload = null), input().copy(evidence = null), input().copy(reference = null),
              input().copy(storageId = null.asInstanceOf[InternalModelHistoryStorageId]),
              input().copy(evidence = Vector(null)),
              input().copy(evidence = Vector(InternalModelEvidenceInput(null, InternalModelEvidenceKind.Prompt, InternalModelEvidenceInputPayload.Raw(_raw_sentinel)))),
              input().copy(evidence = Vector(InternalModelEvidenceInput(reference("evidence", 7L), null, InternalModelEvidenceInputPayload.Raw(_raw_sentinel)))),
              input().copy(evidence = Vector(InternalModelEvidenceInput(reference("evidence", 7L), InternalModelEvidenceKind.Narrative, null))),
              input(payload = InternalModelRetainedHistoryPayload.Evidence(null)),
              input(payload = InternalModelRetainedHistoryPayload.Review(reference("same", 1L), reference("same", 1L), reference("diff", 2L), null)))
            When("wrong-kind routes and malformed input graphs are requested")
            val results = wrongroutes.map(_()) ++ malformed.map(adapter.record(_))
            Then("no rejected graph reaches Entity work and raw evidence is not echoed")
            results.foreach(_invalid)
            results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
            probe.calls shouldBe empty
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
          } finally db.close()
        }}
      }

      "A5 reject oversized raw evidence counts and encoded documents before persistence" in {
        Given("sixty-five evidence entries, oversized raw bodies, oversized logical metadata and invalid safe identities")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val probe = new StoreProbe()
            val adapter = api(db.core, capture, Some(probe))
            val raw = InternalModelEvidenceInput(reference("raw", 7L), InternalModelEvidenceKind.Prompt,
              InternalModelEvidenceInputPayload.Raw(_raw_sentinel + "x" * 1048577))
            val unavailable = InternalModelEvidenceInput(reference("missing", 7L), InternalModelEvidenceKind.ExternalEvidence, InternalModelEvidenceInputPayload.Unavailable)
            val inputs = Vector(input(entries = Vector(raw)), input(entries = Vector.fill(65)(unavailable)),
              input().copy(reference = reference("λ" * 600000, 103L)),
              input(entries = Vector(InternalModelEvidenceInput(reference("provider", 7L), InternalModelEvidenceKind.ProviderIdentity,
                InternalModelEvidenceInputPayload.SafeIdentity("identity contains secret text " + _raw_sentinel)))))
            When("bounded record admission is requested before optional storage")
            val results = inputs.map(adapter.record(_))
            Then("both policy and encoded byte limits reject without a store call or raw diagnostic")
            results.foreach(_invalid)
            results.foreach(result => _diagnostic(result) should not include (_raw_sentinel))
            probe.calls shouldBe empty
          } finally db.close()
        }}
      }

      "A6 require explicit exact live supersession endpoints through the authenticated store route" in {
        Given("two authenticated independently selected Evidence accounts and an explicit relation input")
        withCapture { (_, capture, _) => withDatabase { path =>
          val db = database(path)
          try {
            val adapter = api(db.core, capture, Some(db.store))
            val previous = _retained(fixtureValue(adapter.record(input(1L))))._1
            val successor = _retained(fixtureValue(adapter.record(input(2L))))._1
            val relationinput = input(3L, InternalModelRetainedHistoryPayload.Supersession(previous.reference, successor.reference))
            When("missing, mismatched and cross-scope endpoint pairs precede one exact live pair")
            val absent = adapter.record(relationinput)
            val wrong = adapter.record(relationinput, Some(InternalModelHistorySupersessionEndpoints(previous, successor.copy(reference = reference("wrong", 103L)))))
            val crossscope = adapter.record(relationinput, Some(InternalModelHistorySupersessionEndpoints(previous, successor.copy(scope = scope.copy(componentIdentity = "other")))))
            val accepted = adapter.record(relationinput, Some(InternalModelHistorySupersessionEndpoints(previous, successor)))
            val relationselection = _retained(fixtureValue(accepted))._1
            fixtureValue(adapter.delete(previous))
            val later = adapter.read(relationselection)
            Then("only the exact explicit live relation is written and later removal grants no rewritten current authority")
            Vector(absent, wrong, crossscope).foreach(_invalid)
            later.toOption shouldBe accepted.toOption
          } finally db.close()
        }}
      }

      "A7 preserve configured unavailability while still rejecting invalid input" in {
        Given("an authorized server with no optional Entity store")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val adapter = api(db.core, capture, None)
            When("every history operation is requested without configured storage")
            val results = _operations.filterNot(_ == "resume").map(operation => _invoke(adapter, operation, request))
            val malformed = adapter.record(input().copy(payload = null))
            Then("Unavailable stays explicit, invalid inputs still reject and no memory persistence is invented")
            results.map(_.toOption) shouldBe Vector.fill(6)(Some(InternalModelHistoryResult.Unavailable))
            _invalid(malformed)
            db.entity(selection()) shouldBe None
          } finally db.close()
        }}
      }
    }

    "package-only continuation and source immutability" which {
      "R1 return the same complete all-eight gate report for absent unavailable unrelated and stale history with zero probes" in {
        Given("the substantive all-eight fixture and independently supplied exact decision requirements")
        withCapture { (root, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val before = InternalModelContinuationFixture.treeBytes(root)
            val expected = fixtureValue(InternalModelContinuationActionGate.evaluateVerified(capture, request))
            val unrelated = history(recordReference = reference("unrelated history", 999L))
            val stale = history(recordSubject = subject.copy(subjectRevision = reference("stale subject", 1L).recordRevision))
            val probes = Vector(new StoreProbe(InternalModelHistoryResult.Unavailable),
              new StoreProbe(InternalModelHistoryResult.Retained(selection(2L, unrelated.reference), unrelated, time)),
              new StoreProbe(InternalModelHistoryResult.Retained(selection(3L, stale.reference), stale, time.minusSeconds(2592001L))))
            val stores: Vector[Option[InternalModelRetainedHistoryStore]] = Vector(None) ++ probes.map(probe => Some(probe))
            When("resume receives the same capture/request with each optional history configuration")
            val results = stores.map(store => api(db.core, capture, store).resume(request))
            Then("every complete typed report is identical and the store is never called even for health")
            expected.eligibility shouldBe InternalModelContinuationEligibility.Eligible
            expected.action shouldBe Some(InternalModelContinuationAction.InspectProjections)
            expected.state.continuity.binding.views should have size 8
            results.map(_.toOption) shouldBe Vector.fill(stores.size)(Some(expected))
            probes.foreach(_.calls shouldBe empty)
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
            InternalModelContinuationFixture.treeBytes(root) shouldBe before
          } finally db.close()
        }}
      }

      "R2 keep missing independent decisions and human approval incomplete despite favorable history" in {
        Given("accepted review and selected-proposal history with no independently supplied decision or human input")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val favorable = Vector(history(InternalModelRetainedHistoryPayload.Review(reference("review", 7L),
              reference("candidate", 11L), reference("diff", 17L), InternalModelRetainedReviewOutcome.AcceptedAsReview)),
              history(InternalModelRetainedHistoryPayload.Alternative(reference("proposal", 23L), reference("alternative", 31L),
                InternalModelRetainedAlternativeDisposition.SelectedAsProposal)))
            val humanstage = _cursor_capture(capture, _.copy(currentStage = "human-decision-recorded",
              nextPermittedAction = Some("handoff-approved-candidate")))
            val cases = Vector(capture -> request.copy(requireddecisions = None), humanstage -> request.copy(requireddecisions = None))
            cases.foreach { case (selectedcapture, independent) => favorable.foreach { history =>
              val probe = new StoreProbe(InternalModelHistoryResult.Retained(selection(9L, history.reference), history, time))
              val expected = fixtureValue(InternalModelContinuationActionGate.evaluateVerified(selectedcapture, independent))
              val adapter = api(db.core, selectedcapture, Some(probe))
              When("resume is requested with favorable historical opinion but missing independent evidence")
              val resumed = adapter.resume(independent)
              Then("the complete typed incomplete report remains unchanged and history never supplies actual human approval")
              fixtureValue(resumed) shouldBe expected
              expected.eligibility shouldBe InternalModelContinuationEligibility.Incomplete
              expected.action shouldBe None
              expected.approval shouldBe None
              expected.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.MissingPrerequisite)
              probe.calls shouldBe empty
            }}
          } finally db.close()
        }}
      }

      "R3 preserve every identity revision scope and selection problem dimension from the action gate" in {
        Given("an independent request contradicting package/project/carrier/realization/scope and artifact selection")
        withCapture { (_, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val independent = request.copy(packagereference = packageReference.copy(
              packageId = InternalModelPackageId.from("ffffffff-ffff-ffff-ffff-ffffffffffff").toOption.get,
              projectNamespace = InternalModelProjectToken.from("org.other").toOption.get,
              projectId = InternalModelProjectToken.from("other").toOption.get), carrierrevision = 32L,
              realizationreference = reference("other realization", 999L), scope = scope.copy(componentIdentity = "other"),
              requireddecisions = None)
            val probe = new StoreProbe()
            val expected = fixtureValue(InternalModelContinuationActionGate.evaluateVerified(capture, independent))
            When("the runtime-internal adapter delegates the exact independent request")
            val result = api(db.core, capture, Some(probe)).resume(independent)
            Then("the entire typed report and all gate dimensions survive without storage or substitute action")
            fixtureValue(result) shouldBe expected
            expected.problems.map(_.dimension) should contain allOf("packageId", "projectNamespace", "projectId", "carrierrevision",
              "realizationreference.recordId", "realizationreference.recordRevision", "scope")
            expected.eligibility shouldBe InternalModelContinuationEligibility.Inconsistent
            probe.calls shouldBe empty
          } finally db.close()
        }}
      }

      "R4 leave package source cursor canonical CML bytes and the file set unchanged across all API operations" in {
        Given("one actual source package and a separate task-private SQLite output root")
        withCapture { (root, capture, request) => withDatabase { path =>
          val db = database(path)
          try {
            val before = InternalModelContinuationFixture.treeBytes(root)
            val adapter = api(db.core, capture, Some(db.store))
            When("all authorized API operation families run through their real storage or package-only branch")
            val proposed = _retained(fixtureValue(adapter.propose(input(1L, InternalModelRetainedHistoryPayload.Proposal(reference("candidate", 11L), projection)))))._1
            val reviewed = _retained(fixtureValue(adapter.review(input(2L, payloads.head))))._1
            val recorded = _retained(fixtureValue(adapter.record(input(3L))))._1
            val read = adapter.read(recorded)
            val resume = adapter.resume(request)
            val deleted = adapter.delete(proposed)
            db.clock.set(time.plusSeconds(2592000L))
            val expired = adapter.expire(reviewed)
            val after = InternalModelContinuationFixture.treeBytes(root)
            Then("the observed source bytes and paths are identical and only owned SQLite history outputs change")
            Vector(read.isSuccess, resume.isSuccess, deleted.isSuccess, expired.isSuccess) shouldBe Vector.fill(4)(true)
            after shouldBe before
            after.keySet shouldBe before.keySet
            after("src/main/cml/main.cml") shouldBe before("src/main/cml/main.cml")
            after("src/main/internal-model/resume.yaml") shouldBe before("src/main/internal-model/resume.yaml")
            db.providers.calls shouldBe 0
            db.sources.calls shouldBe 0
          } finally db.close()
        }}
      }
    }

    "the actual generated protocol consumer" which {
      "X1 exclude all twenty-eight generated history operations from constructed protocol listing and resolution" in {
        Given("the actual implementation Factory and generated Entity, Aggregate and View operation owners")
        withActualComponent { component =>
          val originals = Vector(CbdSupportComponent.EntityService, CbdSupportComponent.AggregateService,
            CbdSupportComponent.ViewService)
          val inventories = originals.map { service =>
            service.name -> service.operations.operations.toVector.map(_.name).filter(_.contains("InternalModelHistoryEntry"))
          }.toMap
          val services = component.core.protocol.services.services
          When("the constructed protocol is listed and every original generated history operation is resolved with and without its service owner")
          val listed = services.map(service => service.name -> service.operations.operations.toVector.map(_.name)).toMap
          val resolutions = inventories.toVector.flatMap { case (service, operations) => operations.map { operation =>
            component.core.protocolLogic.makeOperationRequest(Request.ofOperation(operation).copy(service = Some(service)))
          }}
          val unqualified = inventories.values.toVector.flatten.distinct.map { operation =>
            component.core.protocolLogic.makeOperationRequest(Request.ofOperation(operation))
          }
          Then("the original generated inventories exactly match all twenty-eight exclusions and every listed or resolved history route is absent")
          inventories.keySet shouldBe _exclusions.keySet
          inventories.map { case (service, operations) => service -> operations.size } shouldBe Map("entity" -> 12, "aggregate" -> 6, "view" -> 10)
          _exclusions.values.map(_.size).sum shouldBe 28
          inventories.foreach { case (service, operations) =>
            operations.sorted shouldBe _exclusions(service).sorted
            listed(service).toSet.intersect(operations.toSet) shouldBe empty
          }
          listed.values.flatten.toSet.intersect(inventories.values.flatten.toSet) shouldBe empty
          (resolutions ++ unqualified).foreach { result =>
            result match {
              case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe Taxonomy.operationNotFound
              case _ => fail("An excluded history operation resolved")
            }
          }
        }
      }

      "X2 preserve catalog and representative Entity Aggregate View operations and service metadata" in {
        Given("the same actual Factory assembly, six generated service specifications and an independent empty-protocol framework baseline")
        withActualComponent { component =>
          val originals = Vector(CbdSupportComponent.CbdRetrievalService, CbdSupportComponent.CbdCatalogAdminService,
            CbdSupportComponent.CbdReviewAdminService, CbdSupportComponent.AggregateService,
            CbdSupportComponent.ViewService, CbdSupportComponent.EntityService)
          val baseline = Component.Core.create(CbdSupportComponent.name, CbdSupportComponent.componentId,
            ComponentInstanceId.default(CbdSupportComponent.componentId), Protocol.empty)
          When("all assembled services and their retained operation definitions are observed")
          val defaults = baseline.protocol.services.services
          val assembled = component.core.protocol.services.services
          val listed = assembled.map(service => service.name -> service.operations.operations.toVector.map(_.name)).toMap
          Then("the six generated services and unchanged meta/system defaults retain exact order, content, metadata and operations")
          defaults.map(_.name) shouldBe Vector("meta", "system")
          assembled.map(_.name) shouldBe originals.map(_.name) ++ defaults.map(_.name)
          assembled.take(originals.size).zip(originals).foreach { case (actual, original) =>
            actual.specification.content shouldBe original.specification.content
            actual.specification.metadata shouldBe original.specification.metadata
            actual.specification.useDefault shouldBe original.specification.useDefault
            val excluded = _exclusions.getOrElse(original.name, Vector.empty).toSet
            actual.operations.operations.toVector shouldBe original.operations.operations.toVector.filterNot(operation => excluded.contains(operation.name))
          }
          assembled.drop(originals.size).map(_.specification) shouldBe defaults.map(_.specification)
          listed("CbdRetrieval") should contain("searchComponents")
          listed("entity") should contain("loadReviewDiagnosisRecord")
          listed("aggregate") should contain("loadReviewDiagnosis")
          listed("view") should contain("loadReviewDiagnosisByView")
          listed("view") should contain allOf("loadReviewDiagnosisSummary", "searchReviewDiagnosisSummary",
            "searchReviewDiagnosisSummaryRecord", "loadReviewDiagnosisDetail", "searchReviewDiagnosisDetail",
            "searchReviewDiagnosisDetailRecord")
        }
      }
    }
  }

  private def _invoke(adapter: InternalModelContinuationApi, operation: String,
    request: InternalModelContinuationRequest, raw: Boolean = false): Consequence[Any] = {
    val entries = if (raw) Vector(InternalModelEvidenceInput(reference("raw prompt", 7L), InternalModelEvidenceKind.Prompt,
      InternalModelEvidenceInputPayload.Raw(_raw_sentinel))) else Vector.empty
    operation match {
      case "read" => adapter.read(selection())
      case "resume" => adapter.resume(request)
      case "propose" => adapter.propose(input(payload = InternalModelRetainedHistoryPayload.Proposal(reference("candidate", 11L), projection), entries = entries))
      case "review" => adapter.review(input(payload = payloads.head, entries = entries))
      case "record" => adapter.record(input(entries = entries))
      case "expire" => adapter.expire(selection())
      case "delete" => adapter.delete(selection())
    }
  }
  private def _allowed(operation: String): Set[String] = operation match {
    case "read" | "resume" => Set("viewer", "reviewer", "operator", "admin")
    case "propose" | "review" | "record" => Set("reviewer", "operator", "admin")
    case "expire" | "delete" => Set("operator", "admin")
  }
  private def _cursor_capture(capture: InternalModelVerifiedContinuationPackage,
    change: InternalModelResumeCursor => InternalModelResumeCursor): InternalModelVerifiedContinuationPackage = {
    val artifacts = capture.artifacts.map { artifact =>
      if (artifact.context.reference.role != InternalModelArtifactRole.Resume) artifact
      else artifact.copy(bytes = Some(InternalModelResumeCursorCodec.encode(change(
        InternalModelResumeCursorCodec.decode(artifact.bytes.get).toOption.get))))
    }
    InternalModelContinuationFixture.withCapturedInventory(capture, artifacts)
  }
  private def _record(document: InternalModelHistoryDocument): InternalModelRetainedHistoryRecord = document.content match {
    case InternalModelHistoryDocumentState.Retained(record, _) => record
    case _ => fail("Expected retained record")
  }
  private def _retained(result: InternalModelHistoryResult): (InternalModelHistorySelection, InternalModelRetainedHistoryRecord) = result match {
    case InternalModelHistoryResult.Retained(selection, record, _) => selection -> record
    case _ => fail("Expected retained result")
  }
  private def _invalid[A](result: Consequence[A]): Unit = result match {
    case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe Conclusion.operationInvalid("expected").observation.taxonomy
    case _ => fail("Expected structured invalid operation")
  }
  private def _denied[A](result: Consequence[A]): Unit = result match {
    case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe Conclusion.securityPermissionDenied("expected").observation.taxonomy
    case _ => fail("Expected structured security denial")
  }
  private def _diagnostic[A](result: Consequence[A]): String = result match {
    case Consequence.Failure(conclusion) => conclusion.show + conclusion.display
    case _ => ""
  }
  private final case class SpecPrincipal(id: PrincipalId, attributes: Map[String, String]) extends Principal
  private object ClockForSpec {
    def apply(time: Instant): java.time.Clock = java.time.Clock.fixed(time, java.time.ZoneOffset.UTC)
  }
}
