package zio.temporal

import java.{util => ju}
import scala.collection.Factory

trait ZSearchAttributeMetaCollectionInstances {
  implicit def iterable[Coll[x] <: Iterable[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword, String],
    factory:           Factory[V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ZSearchAttribute.Keyword, ju.List[String]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.iterator.to(factory))(_.iterator.to(List))
}
