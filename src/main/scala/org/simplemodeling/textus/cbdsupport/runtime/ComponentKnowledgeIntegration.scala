package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}

import scala.util.control.NonFatal

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeManifestConsumerContract,
  ComponentKnowledgeManifestConsumerContractCodec
}

/**
 * Admits only the value-only consumer contract supplied by a validated,
 * declared Component knowledge carrier.
 */
/*
 * @since   Aug. 26, 2026
 * @version Oct. 17, 2026
 */
object ComponentKnowledgeIntegration {
  final case class Input(
    carrier: Option[ComponentKnowledgeCarrier],
    consumerContractBytes: Vector[Byte],
    expectedComponentId: ComponentId,
    expectedLogicalRelease: String
  )

  sealed trait Result

  /**
   * A validated contract plus its small declaration value.  The result never
   * retains the carrier bytes; the declared schemas and logical path remain
   * available for safe exact-evidence projection.
   */
  final case class Admitted(
    value: ComponentKnowledgeManifestConsumerContract,
    carrier: ComponentKnowledgeCarrier
  ) extends Result

  case object Absent extends Result

  final case class Rejected(reason: String) extends Result

  def admit(input: Input): Result =
    input.carrier match {
      case None => Absent
      case Some(carrier) =>
        _validate_carrier(carrier) match {
          case Left(reason) => Rejected(reason)
          case Right(validated) =>
            _decode_contract(input.consumerContractBytes) match {
              case Left(reason) => Rejected(reason)
              case Right(contract) if contract.componentId != input.expectedComponentId =>
                Rejected("Component knowledge consumer contract component identity does not match the expected identity")
              case Right(contract) if contract.logicalRelease != input.expectedLogicalRelease =>
                Rejected("Component knowledge consumer contract logical release does not match the expected release")
              case Right(contract) => Admitted(contract, validated)
            }
        }
    }

  private def _validate_carrier(
    carrier: ComponentKnowledgeCarrier
  ): Either[String, ComponentKnowledgeCarrier] =
    try {
      ComponentKnowledgeCarrier.validateC(carrier).toOption.toRight(
        "Component knowledge carrier declaration is invalid"
      )
    } catch {
      case NonFatal(error) =>
        Left(s"Component knowledge carrier declaration is invalid: ${_message(error)}")
    }

  private def _decode_contract(
    bytes: Vector[Byte]
  ): Either[String, ComponentKnowledgeManifestConsumerContract] =
    try {
      val text = StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes.toArray))
        .toString
      ComponentKnowledgeManifestConsumerContractCodec.decodeC(text).toOption.toRight(
        "Component knowledge consumer contract is invalid"
      )
    } catch {
      case _: CharacterCodingException =>
        Left("Component knowledge consumer contract is not valid UTF-8")
      case NonFatal(error) =>
        Left(s"Component knowledge consumer contract is invalid: ${_message(error)}")
    }

  private def _message(error: Throwable): String =
    Option(error.getMessage).getOrElse(error.getClass.getSimpleName)
}
