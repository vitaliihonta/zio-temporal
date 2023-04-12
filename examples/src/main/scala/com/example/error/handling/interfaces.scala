package com.example.error.handling

import zio.temporal._

@activityInterface
trait ArithmeticActivity {
  def divide(x:   Int, y: Int): Int
  def multiply(x: Int, y: Int): Int
}

@workflowInterface
trait MathWorkflow {
  @workflowMethod
  def formula(a: Int): Int
}
