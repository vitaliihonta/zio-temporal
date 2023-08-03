package zio.temporal

trait ZSearchAttributeMetaEnumInstances {

  /** Provides an attribute meta for old [[scala.Enumeration]]
    */
  def scalaEnumeration(enum: Enumeration): ZSearchAttributeMeta.Of[enum.Value, String] =
    new ZSearchAttributeMeta.KeywordMeta[enum.Value](_.toString, raw => enum.withName(raw))
}
