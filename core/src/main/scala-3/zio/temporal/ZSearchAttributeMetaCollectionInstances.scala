package zio.temporal

import java.{util => ju}
import scala.collection.Factory

trait ZSearchAttributeMetaCollectionInstances {
  implicit def traversableInstance[Coll[x] <: IterableOnce[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword],
    factory:           Factory[V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ZSearchAttribute.Plain[ju.List[String]]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.iterator.to(factory), _.iterator.to(List))
}
