package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.security.MessageDigest

import scala.util.control.NonFatal

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeManifestConsumerContract,
  ComponentKnowledgeManifestConsumerContractCodec
}

/**
 * Admits only the value-only consumer contract supplied by a validated,
 * digest-bound Component knowledge carrier.
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
   * retains the carrier bytes; the declared logical path and digest remain
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
            val digest = _sha256(input.consumerContractBytes)
            if (digest != validated.sha256)
              Rejected("Component knowledge consumer contract digest does not match its carrier")
            else
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

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes.toArray)
      .map(byte => f"${byte & 0xff}%02x")
      .mkString

  private def _message(error: Throwable): String =
    Option(error.getMessage).getOrElse(error.getClass.getSimpleName)
}
