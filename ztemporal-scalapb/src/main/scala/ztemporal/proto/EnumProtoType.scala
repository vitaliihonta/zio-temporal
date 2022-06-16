package ztemporal.proto

import ztemporal.proto.enumeratum.EnumeratumProtoType

/** Provides a conversion between scalapb generated enums and enumeratum It's an optional dependency which won't be
  * added to the classpath unless you use it
  */
object EnumProtoType extends EnumeratumProtoType
