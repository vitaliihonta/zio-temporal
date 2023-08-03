package zio.temporal

import scala.collection.generic.CanBuildFrom
import java.{util => ju}

trait ZSearchAttributeMetaCollectionInstances {
  implicit def traversableInstance[Coll[x] <: Iterable[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword],
    cbf:               CanBuildFrom[List[V], V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ZSearchAttribute.Plain[ju.List[String]]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.to[Coll], _.toList)
}
