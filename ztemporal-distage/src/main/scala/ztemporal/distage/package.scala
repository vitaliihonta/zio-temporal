package ztemporal

package object distage {

  /** Alias for workflow creator function
    * @tparam A
    *   workflow interface implementation
    */
  type ZWorkflowFactory[+A] = () => A
}
