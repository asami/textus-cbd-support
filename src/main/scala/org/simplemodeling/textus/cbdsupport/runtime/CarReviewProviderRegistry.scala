package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewProviderRegistryFailure(code: String, message: String)

final case class CarReviewProviderDiscoveryRequest(
  capabilityIds: Vector[ReviewCapabilityId],
  maxProviders: Int
)

final case class CarReviewRegisteredProvider(
  descriptor: CarReviewProviderDescriptor,
  runner: CarReviewProviderRunner
)

/**
 * CBD-owned registry for explicitly admitted local provider implementations.
 * It has no discovery fallback: callers receive only descriptors that were
 * registered with a strict v1 provider descriptor and match every requested
 * capability.
 */
final class CarReviewProviderRegistry {
  private var _providers = Map.empty[ReviewProviderIdentity, CarReviewRegisteredProvider]

  def register(
    descriptorDocument: String,
    runner: CarReviewProviderRunner
  ): Either[CarReviewProviderRegistryFailure, CarReviewProviderDescriptor] = synchronized {
    CarReviewProviderBundleAdmission.describeDescriptor(descriptorDocument).left.map(error => CarReviewProviderRegistryFailure(error, s"Provider descriptor was not admitted: $error.")).flatMap { descriptor =>
      _providers.get(descriptor.provider) match {
        case Some(current) if current.descriptor == descriptor && (current.runner eq runner) => Right(current.descriptor)
        case Some(current) if current.descriptor == descriptor => Left(CarReviewProviderRegistryFailure("provider-runner-conflict", "Provider identity is already registered with a different runner."))
        case Some(_) => Left(CarReviewProviderRegistryFailure("provider-registration-conflict", "Provider identity is already registered with a different descriptor."))
        case None =>
          _providers = _providers.updated(descriptor.provider, CarReviewRegisteredProvider(descriptor, runner))
          Right(descriptor)
      }
    }
  }

  def discover(request: CarReviewProviderDiscoveryRequest): Either[CarReviewProviderRegistryFailure, Vector[CarReviewProviderDescriptor]] = synchronized {
    if request.maxProviders <= 0 then Left(CarReviewProviderRegistryFailure("invalid-provider-discovery-bound", "Provider discovery bound must be positive."))
    else if request.capabilityIds.isEmpty || request.capabilityIds.distinct.size != request.capabilityIds.size then Left(CarReviewProviderRegistryFailure("invalid-provider-capability-request", "Provider discovery requires distinct capability IDs."))
    else {
      val descriptors = _providers.valuesIterator.map(_.descriptor).filter { descriptor =>
        val capabilities = descriptor.capabilities.map(_.id).toSet
        request.capabilityIds.forall(capabilities.contains)
      }.toVector.sortBy(descriptor => (descriptor.provider.id.value, descriptor.provider.version.value)).take(request.maxProviders)
      Right(descriptors)
    }
  }

  def runnerFor(provider: ReviewProviderIdentity): Option[CarReviewProviderRunner] = synchronized {
    _providers.get(provider).map(_.runner)
  }

  def registrationFor(provider: ReviewProviderIdentity): Option[CarReviewRegisteredProvider] = synchronized {
    _providers.get(provider)
  }
}
