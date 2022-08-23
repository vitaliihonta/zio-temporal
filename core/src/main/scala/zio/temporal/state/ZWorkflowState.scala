package zio.temporal.state

sealed trait ZWorkflowState[A] {

  /** Replaces the state value
    *
    * @param value
    *   new state value
    * @return
    *   this state updated
    */
  def :=(value: A): this.type =
    setTo(value)

  /** Replaces the state value
    *
    * @param value
    *   new state value
    * @return
    *   this state updated
    */
  def setTo(value: A): this.type

  /** Updates the state value
    *
    * @param f
    *   updating function
    * @return
    *   this state updated
    */
  def update(f: A => A): this.type

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
  def updateWhen(pf: PartialFunction[A, A]): this.type

  /** Takes a snapshot of the state or throws an error
    *
    * @return
    *   the state value
    */
  @throws[NoSuchElementException]
  def snapshot: A = snapshotOrElse(throw new NoSuchElementException("State was not initialized"))

  /** Returns true if this State is initialized '''and''' the predicate $p returns true when applied to this state
    * value. Otherwise, returns false.
    *
    * @param p
    *   the predicate to test
    */
  def exists(p: A => Boolean): Boolean

  /** Returns true if this state is uninitialized '''or''' the predicate p returns true when applied to this state
    * value.
    *
    * @param p
    *   the predicate to test
    */
  def forall(p: A => Boolean): Boolean

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
  def snapshotOrElse(default: => A): A

  /** Converts this state to [[Option]]
    *
    * @return
    *   the state value or [[None]]
    */
  def toOption: Option[A]

  /** Converts this state to [[Either]]
    *
    * @tparam E
    *   error type
    * @param left
    *   error value which will be used when state is not initialized
    * @return
    *   the state value or [[Left]]
    */
  def toEither[E](left: => E): Either[E, A]
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
  def make[A](value: A): ZWorkflowState.Required[A] = new ZWorkflowState.Required[A](value)

  /** Creates an uninitialized state
    *
    * @tparam A
    *   state type
    * @return
    *   uninitialized state
    */
  def empty[A]: ZWorkflowState.Optional[A] = new ZWorkflowState.Optional[A](None)

  final case class Optional[A] private[zio] (private var underlying: Option[A]) extends ZWorkflowState[A] {

    /** Replaces the state value
      *
      * @param value
      *   new state value
      * @return
      *   this state updated
      */
    override def setTo(value: A): this.type = {
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
    override def update(f: A => A): this.type = {
      underlying = underlying.map(f)
      this
    }

    /** Updates the state when partial function matches
      *
      * @param pf
      *   updating function
      * @return
      *   this state updated
      */
    override def updateWhen(pf: PartialFunction[A, A]): this.type = {
      underlying = underlying.map(value => pf.applyOrElse[A, A](value, identity[A]))
      this
    }

    /** Checks whenever the state is initialized.
      *
      * @return
      *   true if not initialized
      */
    def isEmpty: Boolean =
      underlying.isEmpty

    /** Checks whenever the state is initialized.
      *
      * @return
      *   true if initialized
      */
    def nonEmpty: Boolean = !isEmpty

    /** Returns true if this State is initialized '''and''' the predicate $p returns true when applied to this state
      * value. Otherwise, returns false.
      *
      * @param p
      *   the predicate to test
      */
    override def exists(p: A => Boolean): Boolean =
      underlying.exists(p)

    /** Returns true if this state is uninitialized '''or''' the predicate p returns true when applied to this state
      * value.
      *
      * @param p
      *   the predicate to test
      */
    override def forall(p: A => Boolean): Boolean =
      underlying.forall(p)

    /** Takes a snapshot of the state or returns the provided default value
      *
      * @param default
      *   the default value
      * @return
      *   the state or default value
      */
    override def snapshotOrElse(default: => A): A =
      underlying.getOrElse(default)

    /** Converts this state to [[Option]]
      *
      * @return
      *   the state value or [[None]]
      */
    override def toOption: Option[A] =
      underlying

    /** Converts this state to [[Either]]
      *
      * @tparam E
      *   error type
      * @param left
      *   error value which will be used when state is not initialized
      * @return
      *   the state value or [[Left]]
      */
    override def toEither[E](left: => E): Either[E, A] =
      underlying.toRight(left)
  }

  final case class Required[A] private[zio] (private var underlying: A) extends ZWorkflowState[A] {

    /** Replaces the state value
      *
      * @param value
      *   new state value
      * @return
      *   this state updated
      */
    override def setTo(value: A): this.type = {
      underlying = value
      this
    }

    /** Updates the state value
      *
      * @param f
      *   updating function
      * @return
      *   this state updated
      */
    override def update(f: A => A): this.type = {
      underlying = f(underlying)
      this
    }

    /** Updates the state when partial function matches
      *
      * @param pf
      *   updating function
      * @return
      *   this state updated
      */
    override def updateWhen(pf: PartialFunction[A, A]): this.type = {
      pf.applyOrElse[A, A](underlying, identity[A])
      this
    }

    /** Returns true if this State is initialized '''and''' the predicate $p returns true when applied to this state
      * value. Otherwise, returns false.
      *
      * @param p
      *   the predicate to test
      */
    override def exists(p: A => Boolean): Boolean =
      p(underlying)

    /** Returns true if this state is uninitialized '''or''' the predicate p returns true when applied to this state
      * value.
      *
      * @param p
      *   the predicate to test
      */
    override def forall(p: A => Boolean): Boolean =
      p(underlying)

    /** Takes a snapshot of the state or returns the provided default value
      *
      * @param default
      *   the default value
      * @return
      *   the state or default value
      */
    override def snapshotOrElse(default: => A): A = underlying

    /** Converts this state to [[Option]]
      *
      * @return
      *   the state value or [[None]]
      */
    override def toOption: Option[A] = Some(underlying)

    /** Converts this state to [[Either]]
      *
      * @tparam E
      *   error type
      * @param left
      *   error value which will be used when state is not initialized
      * @return
      *   the state value or [[Left]]
      */
    override def toEither[E](left: => E): Either[E, A] = Right(underlying)
  }
}
