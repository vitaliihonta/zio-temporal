package zio.temporal

trait ZSearchAttributeMetaEnumInstances {

  /** Provides an attribute meta for old [[scala.Enumeration]]
    */
  def enumeration(enum: Enumeration): ZSearchAttributeMeta.Of[enum.Value, ZSearchAttribute.Keyword, String] =
    new ZSearchAttributeMeta.KeywordMeta[enum.Value](_.toString, raw => enum.withName(raw))
}
