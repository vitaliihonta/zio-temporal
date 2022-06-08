package ztemporal

import scala.annotation.StaticAnnotation

/** Indicates that public method is not recommended for use outside of the ztemporal itself
  */
class internalApi extends StaticAnnotation
