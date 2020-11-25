package jsoft.flush4s.core.impl

import jsoft.flush4s.core._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class PrependImpl[A](item: A, src: Flush[A]) extends Flush[A] {
  override def call(subs: Subscriber[A]): Unit = {
    implicit val ec = subs.executionContext
    subs.onNext(item).recoverWith { case t => subs.onError(t) }.onComplete {
      case Failure(exception) => subs.onAbort(exception)
      case Success(value) =>
        value match {
          case Continue =>
            src.call(new Subscriber[A] {

              override def executionContext: ExecutionContext = ec

              override def onNext(next: A): Future[Ack] = subs.onNext(next)

              override def onComplete(): Unit = subs.onComplete()

              override def onError(t: Throwable): Future[Ack] = subs.onError(t)

              override def onAbort(t: Throwable): Unit = subs.onAbort(t)
            })
          case Stop => subs.onComplete()
        }
    }
  }
}
