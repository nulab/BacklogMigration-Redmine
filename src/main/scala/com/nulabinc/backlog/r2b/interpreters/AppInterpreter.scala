package com.nulabinc.backlog.r2b.interpreters

import java.io.PrintStream

import backlog4s.dsl.ApiDsl.ApiPrg
import backlog4s.interpreters.AkkaHttpInterpret
import cats.free.Free
import cats.{Monad, ~>}
import cats.implicits._
import com.nulabinc.backlog.migration.common.utils.ConsoleOut
import com.nulabinc.backlog.r2b.interpreters.ConsoleDSL.ConsoleProgram

import scala.concurrent.{ExecutionContext, Future}

sealed trait ConsoleADT[+A]
case class Output(message: String) extends ConsoleADT[PrintStream]

object ConsoleDSL {

  type ConsoleProgram[A] = Free[ConsoleADT, A]
}

case class ConsoleInterpreter()(implicit exc: ExecutionContext) extends (ConsoleADT ~> Future) {

  implicit val monad = implicitly[Monad[Future]]

  def run[A](prg: ConsoleProgram[A]): Future[A] =
    prg.foldMap(this)

  override def apply[A](fa: ConsoleADT[A]): Future[A] = fa match {
    case Output(message) => Future.successful {
      ConsoleOut.println(message)
    }
  }
}

sealed trait AppADT[+A]
case class Pure[A](a: A) extends AppADT[A]
case class Backlog[A](prg: ApiPrg[A]) extends AppADT[A]
case class Console[A](prg: ConsoleProgram[A]) extends AppADT[A]


object AppDSL {

  type AppProgram[A] = Free[AppADT, A]

  def pure[A](a: A): AppProgram[A] =
    Free.liftF(Pure(a))

  def backlog[A](prg: ApiPrg[A]): AppProgram[A] =
    Free.liftF(Backlog(prg))

  def console[A](prg: ConsoleProgram[A]): AppProgram[A] =
    Free.liftF(Console(prg))

}

case class AppInterpreter(backlogInterpreter: AkkaHttpInterpret,
                          consoleInterpreter: ConsoleInterpreter) extends (AppADT ~> Future) {

  override def apply[A](fa: AppADT[A]): Future[A] = fa match {
    case Pure(a) => Future.successful(a)
    case Backlog(prg) => backlogInterpreter.run(prg)
    case Console(prg) => consoleInterpreter.run(prg)
  }
}
