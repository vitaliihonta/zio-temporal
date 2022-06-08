package ztemporal

import izumi.distage.model.definition.ModuleDef
import izumi.distage.testkit.TestConfig
import zio._
import izumi.distage.testkit.scalatest.Spec2
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import ztemporal.testkit.ZTestWorkflowEnvironment

abstract class BaseSpec extends Spec2[IO] with Matchers with EitherValues {

  override protected def config: TestConfig = {
    val superConfig = super.config
    superConfig.copy(
      moduleOverrides = new ModuleDef {
        include(superConfig.moduleOverrides)
        make[ZTestWorkflowEnvironment].fromResource(ZTestWorkflowEnvironment.make())
      }
    )
  }
}
