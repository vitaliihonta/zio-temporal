package zio.temporal

import scala.collection.Factory
import java.{util => ju}

trait ZSearchAttributeMetaCollectionInstances {
  implicit def iterable[Coll[x] <: Iterable[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword, String],
    factory:           Factory[V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ZSearchAttribute.Keyword, ju.List[String]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.iterator.to(factory), _.iterator.to(List))
}
