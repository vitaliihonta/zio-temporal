package zio.temporal

import scala.collection.generic.CanBuildFrom
import java.{util => ju}

trait ZSearchAttributeMetaCollectionInstances {
  implicit def iterable[Coll[x] <: Iterable[x], V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword, String],
    cbf:               CanBuildFrom[List[V], V, Coll[V]]
  ): ZSearchAttributeMeta.Of[Coll[V], ZSearchAttribute.Keyword, ju.List[String]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Coll[V]](_.to[Coll], _.toList)

  implicit def set[V](
    implicit asString: ZSearchAttributeMeta.Of[V, ZSearchAttribute.Keyword, String]
  ): ZSearchAttributeMeta.Of[Set[V], ZSearchAttribute.Keyword, ju.List[String]] =
    ZSearchAttributeMeta.keywordListImpl[V].convert[Set[V]](_.toSet, _.toList)
}
