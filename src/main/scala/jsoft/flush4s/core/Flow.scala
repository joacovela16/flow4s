package jsoft.flush4s.core

import java.util.concurrent.TimeUnit

import jsoft.flush4s.core.impl._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Flow[A] {

  def call(subs: Subscriber[A]): Unit

  final def intersperse(start: A, middle: A, end: A): Flow[A] = IntersperseImpl(this, start, middle, end)

  final def prepend(item: A): Flow[A] = PrependImpl(item, this)

  final def interceptor[B](f: Seq[A] => Flow[B]): Flow[B] = InterceptorImpl(this, f)

  final def append(item: A): Flow[A] = AppendImpl(item, this)

  final def delay(millis: Long): Flow[A] = DelayImpl(millis, this)

  final def map[B](f: A => B): Flow[B] = MapImpl(f, this)

  final def stage[B](f: A => Try[B]): Flow[B] = MapTryImpl(this, f)

  final def conditionalStage[B >: A](stage: A => Try[B])(conditions: (A => Boolean)*): Flow[B] = ConditionalStageImpl(this, stage, conditions)

  final def mapIterable[B, S[x] <: Iterable[x]](f: A => S[B]): Flow[B] = MapIterableImpl(f, this)

  final def flatMap[B](f: A => Flow[B]): Flow[B] = FlatMapImpl(f, this)

  final def foreach(f: A => Unit): Flow[Unit] = ForeachImpl(f, this)

  final def switch[B](cond: (A => Boolean, A => Flow[B])*): Flow[B] = SwitchImpl(this, cond)

  final def recoverWith(f: Throwable => A): Flow[A] = RecoverWithImpl(f, this)

  final def foldLeft[B](start: B)(f: (B, A) => B): Flow[B] = FoldLefImpl(start, f, this)

  final def connect[B](next: Flow[A] => Flow[B]): Flow[B] = next(this)

  final def splitIf(cond: A => Boolean)(f: A => Flow[A]): Flow[A] = SplitIfImpl(cond, f, this)

  final def runSync(duration: Duration = Constants.TIMEOUT): Try[Seq[A]] = Try(Await.result(runAsync, duration))

  final def headSync(duration: Duration = Constants.TIMEOUT): Try[A] = runSync(duration).map(_.head)

  final def runAsync(implicit ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global): Future[Seq[A]] = {

    val promise: Promise[Seq[A]] = Promise[Seq[A]]()

    call(new Subscriber[A] {
      val buffer: mutable.ArrayBuffer[A] = mutable.ArrayBuffer.empty[A]

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future {
        buffer += next
        Continue
      }

      override def onAbort(t: Throwable): Unit = {
        Flow.checker(promise, promise.failure(t))
      }

      override def onComplete(): Unit = Flow.checker(promise, promise.success(buffer))

      override def onError(t: Throwable): Future[Ack] = {
        Future.successful(Stop)
      }
    })

    promise.future
  }

}
object Flow {

  private [core] def checker[T](promise: Promise[T], f: => Unit): Unit =  {
    if (!promise.isCompleted) f
  }

  private [core] def proxy[A](subscriber: Subscriber[A], flush: Flow[A]): Unit = {
    flush.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = subscriber.executionContext

      override def onNext(next: A): Future[Ack] = subscriber.onNext(next)

      override def onComplete(): Unit = {}

      override def onError(t: Throwable): Future[Ack] = subscriber.onError(t)

      override def onAbort(t: Throwable): Unit = subscriber.onAbort(t)
    })
  }

  private [core] def syncMap[A](subs: Subscriber[A], flow: Flow[A]): Future[Ack] = {
    implicit val ec: ExecutionContext = subs.executionContext
    val promise: Promise[Ack] = Promise[Ack]()

    flow.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = subs.onNext(next)

      override def onComplete(): Unit = if (!promise.isCompleted) promise.success(Continue)

      override def onError(t: Throwable): Future[Ack] = {
        if (!promise.isCompleted) promise.failure(t)
        Future.failed(t)
      }

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })

    promise.future
  }

  final def pure[A](f: => A): Flow[A] = fromIterable(f)

  final def fromFuture[A](f: => Future[A]): Flow[A] = {
    subs => {
      implicit val ec: ExecutionContext = subs.executionContext
      f.flatMap(subs.onNext).recoverWith { case t => subs.onError(t) }.onComplete {
        case Failure(exception) => subs.onAbort(exception)
        case _ => subs.onComplete()
      }
    }
  }

  final def fromSeq[A](items: Seq[A]): Flow[A] = fromIterable(items: _*)

  final def fromIterable[A](items: A*): Flow[A] = {
    (subscriber: Subscriber[A]) => {
      implicit val ec: ExecutionContext = subscriber.executionContext
      val it: Iterator[A] = items.iterator

      def iterable(it: Iterator[A]): Unit = {
        if (it.hasNext) {
          val item: A = it.next()
          subscriber.onNext(item).recoverWith {
            case t: Throwable => subscriber.onError(t)
          }
            .onComplete {
              case Failure(exception) => subscriber.onAbort(exception)
              case Success(value) =>
                value match {
                  case Continue => iterable(it)
                  case _ => subscriber.onComplete()
                }
            }
        } else {
          subscriber.onComplete()
        }
      }

      iterable(it)
    }
  }

  final def raiseError[A](message: String): Flow[A] = subs => {
    subs.onAbort(new RuntimeException(message))
  }

  final def raiseError[A](t: Try[A]): Flow[A] = subs => {
    t match {
      case Failure(exception) => subs.onAbort(exception)
      case Success(value) => subs.onNext(value)
    }
  }

}