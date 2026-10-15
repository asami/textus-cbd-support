package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonPart._nonnull

/*
 * Capture accepted values without reacquisition or native construction.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalog {
  def fromSnapshot(snapshot: Snapshot, schemaVersion: Int): Either[Diagnostic, LiveCatalog] = for {
    original <- _nonnull(snapshot, "/snapshot")
    packages = original.componentPackages.map {
      case ComponentPackage.MetadataOnly(acquired) => PackageProjection(acquired.input, Vector.empty, Vector.empty)
      case ComponentPackage.Native(discovered) => PackageProjection(discovered.acquired.input, discovered.nativeInput.bindings.map(_.target), discovered.entries.map { entry =>
        ResourceProjection(entry.loaded.resourcePath, entry.loaded.projection, entry.admitted.capabilities,
          entry.admitted.realizations.map(r => ResolvedRealizationFact(r.edge, r.target)))
      })
    }
    projection = PortableProjection(schemaVersion, original.reference, original.provenance, packages, original.usage, original.useCase)
    catalog <- CapabilityCatalogBindingPart.bind(projection, CatalogSelection(original.reference, original.provenance))
  } yield {
    val observations = original.components.flatMap { discovered => discovered.entries.flatMap { entry =>
      val p = entry.loaded.projection
      val source = SourceKey(p.source.modelAuthority, p.source.path, p.source.sourceRevision)
      val context = ComponentContextKey(discovered.acquired.input.scopeId, p.component, p.packageCoordinate, entry.loaded.resourcePath, source)
      entry.realizations.map(r => NativeRealizationObservation(DeclarationRecordKey(context, r.realization.edge.declarationKey), r))
    } }
    LiveCatalog(original, catalog, observations)
  }
}
