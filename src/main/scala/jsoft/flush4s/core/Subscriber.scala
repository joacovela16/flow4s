package jsoft.flush4s.core

import scala.concurrent.{ExecutionContext, Future}

trait Subscriber[A] {
  def executionContext: ExecutionContext

  def onNext(next: A): Future[Ack]

  def onComplete(): Unit

  def onError(t: Throwable): Future[Ack]

  def onAbort(t: Throwable): Unit
}
