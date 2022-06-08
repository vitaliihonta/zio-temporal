import zio.ZIO
import zio.blocking.Blocking

package object ztemporal {

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam E ztemporal error type
    * @tparam A the value type
    */
  type ZTemporalIO[+E <: ZTemporalError[_], +A] = ZIO[Blocking, E, A]
}
