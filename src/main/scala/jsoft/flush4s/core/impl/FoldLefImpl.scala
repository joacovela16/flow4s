package jsoft.flush4s.core.impl

import java.util.concurrent.atomic.AtomicReference

import jsoft.flush4s.core.{Ack, Continue, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class FoldLefImpl[A, B](start: B, f: (B, A) => B, src: Flow[A]) extends Flow[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext
    src.call(new Subscriber[A] {

      val result = new AtomicReference[B](start)

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        Future(f(result.get(), next)).map { r =>
          result.set(r)
          Continue
        }
      }

      override def onComplete(): Unit = {
        val toSend: B = result.get()
        subs.onNext(toSend).recoverWith { case t => subs.onError(t) }.onComplete {
          case Failure(exception) => subs.onAbort(exception)
          case _ => subs.onComplete()
        }
      }

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
