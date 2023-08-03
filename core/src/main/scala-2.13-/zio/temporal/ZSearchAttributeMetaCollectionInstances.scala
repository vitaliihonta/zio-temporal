package zio.temporal

import scala.collection.generic.CanBuildFrom
import java.{util => ju}

trait ZSearchAttributeMetaCollectionInstances {
  implicit def traversableInstance[Coll[x] <: Traversable[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, String],
    cbf:               CanBuildFrom[List[V], V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ju.List[String]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.to[Coll], _.toList)
}
