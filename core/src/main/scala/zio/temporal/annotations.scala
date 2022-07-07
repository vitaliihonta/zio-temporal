package zio.temporal

import scala.annotation.StaticAnnotation

/** Indicates that public method is not recommended for use outside of the zio.tempora itself
  */
class internalApi extends StaticAnnotation

/** Indicates that public method is still experimental and may change
  */
class experimentalApi extends StaticAnnotation
