package ztemporal.state

/** Helper instance representing workflow state. Can be either uninitialized or containing value
  *
  * @tparam A
  *   the state value
  */
class ZWorkflowState[A] private[ztemporal] (private var underlying: Option[A]) {

  /** Replaces the state value
    *
    * @param value
    *   new state value
    * @return
    *   this state updated
    */
  def setTo(value: A): this.type = {
    underlying = Some(value)
    this
  }

  /** Updates the state value
    *
    * @param f
    *   updating function
    * @return
    *   this state updated
    */
  def update(f: A => A): this.type = {
    underlying = underlying.map(f)
    this
  }

  /** Updates the state when the predicate holds
    *
    * @param f
    *   updating function
    * @return
    *   this state updated
    */
  def updateIf(p: Boolean)(f: A => A): this.type =
    if (p) update(f)
    else this

  /** Updates the state unless the predicate holds
    *
    * @param f
    *   updating function
    * @return
    *   this state updated
    */
  def updateUnless(p: Boolean)(f: A => A): this.type =
    updateIf(!p)(f)

  /** Updates the state when partial function matches
    *
    * @param pf
    *   updating function
    * @return
    *   this state updated
    */
  def updateWhen(pf: PartialFunction[A, A]): this.type = {
    underlying = underlying.collect(pf)
    this
  }

  /** Takes a snapshot of the state or throws an error
    *
    * @return
    *   the state value
    */
  @throws[NoSuchElementException]
  def snapshot: A = snapshotOrElse(throw new NoSuchElementException("State was not initialized"))

  /** Returns true if this State is initialized '''and''' the predicate $p returns true when applied to this state
    * value. Otherwise, returns false.
    * @param p
    *   the predicate to test
    */
  def exists(p: A => Boolean): Boolean =
    underlying.exists(p)

  /** Returns true if this state is uninitialized '''or''' the predicate p returns true when applied to this state
    * value.
    *
    * @param p
    *   the predicate to test
    */
  def forall(p: A => Boolean): Boolean =
    underlying.forall(p)

  /** Takes a snapshot of the state applying a function to it ors throw an error
    *
    * @param f
    *   arbitrary function
    * @return
    *   the result of function application on this state
    */
  @throws[NoSuchElementException]
  def snapshotOf[B](f: A => B): B = f(snapshot)

  /** Takes a snapshot of the state or returns the provided default value
    *
    * @param default
    *   the default value
    * @return
    *   the state or default value
    */
  def snapshotOrElse(default: => A): A = underlying.getOrElse(default)

  /** Converts this state to [[Option]]
    * @return
    *   the state value or [[None]]
    */
  def toOption: Option[A] = underlying

  /** Converts this state to [[Either]]
    * @tparam E
    *   error type
    * @param left
    *   error value which will be used when state is not initialized
    * @return
    *   the state value or [[Left]]
    */
  def toEither[E](left: => E): Either[E, A] = underlying.toRight(left)
}

object ZWorkflowState {

  /** Initializes the state with a value
    *
    * @tparam A
    *   state type
    * @param value
    *   the state value
    * @return
    *   the state
    */
  def make[A](value: A): ZWorkflowState[A] = new ZWorkflowState[A](Some(value))

  /** Creates an uninitialized state
    *
    * @tparam A
    *   state type
    * @return
    *   uninitialized state
    */
  def empty[A]: ZWorkflowState[A] = new ZWorkflowState[A](None)
}
