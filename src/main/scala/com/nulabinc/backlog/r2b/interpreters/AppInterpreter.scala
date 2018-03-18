package com.nulabinc.backlog.r2b.interpreters

import backlog4s.dsl.ApiDsl.ApiPrg
import backlog4s.interpreters.AkkaHttpInterpret
import backlog4s.streaming.ApiStream.ApiStream
import cats.free.Free
import cats.{Monad, ~>}
import cats.implicits._
import com.nulabinc.backlog.r2b.interpreters.AppDSL.AppProgram
import com.nulabinc.backlog.r2b.interpreters.ConsoleDSL.ConsoleProgram

import scala.concurrent.{ExecutionContext, Future}

sealed trait AppADT[A]
case class Pure[A](a: A) extends AppADT[A]
case class Backlog[A](prg: ApiPrg[A]) extends AppADT[A]
case class BacklogStream[A](prg: ApiStream[A]) extends AppADT[A]
case class Console[A](prg: ConsoleProgram[A]) extends AppADT[A]

object AppDSL {

  type AppProgram[A] = Free[AppADT, A]

  def pure[A](a: A): AppProgram[A] =
    Free.liftF(Pure(a))

  def backlog[A](prg: ApiPrg[A]): AppProgram[A] =
    Free.liftF(Backlog(prg))

  def backlogStream[A](prg: ApiStream[A]): AppProgram[A] =
    Free.liftF(BacklogStream(prg))

  def console[A](prg: ConsoleProgram[A]): AppProgram[A] =
    Free.liftF(Console(prg))

}

case class AppInterpreter(backlogInterpreter: AkkaHttpInterpret,
                          consoleInterpreter: ConsoleInterpreter)
                         (implicit ec: ExecutionContext) extends (AppADT ~> Future) {

  implicit val monad = implicitly[Monad[Future]]

  def run[A](prg: AppProgram[A]): Future[A] =
    prg.foldMap(this)

  override def apply[A](fa: AppADT[A]): Future[A] = fa match {
    case Pure(a) => Future.successful(a)
    case Backlog(prg) => backlogInterpreter.run(prg)
//    case BacklogStream(prg) => backlogInterpreter.runStream(prg)
    case Console(prg) => prg.foldMap(consoleInterpreter)
  }
}
