package com.nulabinc.backlog.r2b.interpreters

import backlog4s.dsl.ApiDsl.ApiPrg
import backlog4s.dsl.BacklogHttpInterpret
import backlog4s.streaming.ApiStream.ApiStream
import cats.free.Free
import cats.~>
import cats.implicits._
import com.nulabinc.backlog.r2b.interpreters.AppDSL.AppProgram
import com.nulabinc.backlog.r2b.interpreters.ConsoleDSL.ConsoleProgram
import monix.execution.{CancelableFuture, Scheduler}
import monix.reactive.Observable
import org.reactivestreams.Subscriber

import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait AppADT[+A]
case class Pure[A](a: A) extends AppADT[A]
case class Backlog[A](prg: ApiPrg[A]) extends AppADT[A]
case class BacklogStream[A](prg: ApiStream[A]) extends AppADT[Observable[Seq[A]]]
case class Console[A](prg: ConsoleProgram[A]) extends AppADT[A]
case class ConsumeStream[A](stream: Observable[A]) extends AppADT[CancelableFuture[Unit]]
case class Exit(exitCode: Int) extends AppADT[Unit]

object AppDSL {

  type AppProgram[A] = Free[AppADT, A]

  def pure[A](a: A): AppProgram[A] =
    Free.liftF(Pure(a))

  def backlog[A](prg: ApiPrg[A]): AppProgram[A] =
    Free.liftF(Backlog(prg))

  def backlogStream[A](prg: ApiStream[A]): AppProgram[Observable[Seq[A]]] =
    Free.liftF[AppADT, Observable[Seq[A]]](BacklogStream(prg))

  def consumeStream[A](stream: Observable[A]): AppProgram[CancelableFuture[Unit]] =
    Free.liftF[AppADT, CancelableFuture[Unit]](ConsumeStream(stream))

  def console[A](prg: ConsoleProgram[A]): AppProgram[A] =
    Free.liftF(Console(prg))

  def exit(reason: String, exitCode: Int): AppProgram[Unit] = {
    for {
      _ <- console(ConsoleDSL.print(reason))
      _ <- Free.liftF(Exit(exitCode))
    } yield ()
  }

}

case class AppInterpreter(backlogInterpreter: BacklogHttpInterpret[Future],
                          consoleInterpreter: ConsoleInterpreter)
                         (implicit ec: Scheduler) extends (AppADT ~> Future) {

  def run[A](prg: AppProgram[A]): Future[A] =
    prg.foldMap(this)

  override def apply[A](fa: AppADT[A]): Future[A] = fa match {
    case Pure(a) => Future.successful(a)
    case Backlog(prg) => backlogInterpreter.run(prg)
    case BacklogStream(stream) => Future.successful {
      Observable.fromReactivePublisher[Seq[A]](
        (s: Subscriber[_ >: Seq[A]]) => {
          backlogInterpreter.runStream(
            stream.map { value =>
              val a = value.asInstanceOf[Seq[A]]
              s.onNext(a) // publish data
              Seq(a)
            }
          ).onComplete {
            case Success(_) => s.onComplete()
            case Failure(ex) => s.onError(ex)
          }
        }
      )
    }
    case Console(prg) => prg.foldMap(consoleInterpreter)
    case Exit(statusCode) => sys.exit(statusCode)
    case ConsumeStream(stream) =>
      stream.runAsyncGetFirst.map(_ => ().asInstanceOf[A])
  }
}

