package com.nulabinc.backlog.r2b.interpreters

import cats.free.Free
import cats.~>
import com.nulabinc.backlog.r2b.dsl.BacklogDSL.BacklogProgram
import com.nulabinc.backlog.r2b.interpreters.AppDSL.AppProgram
import com.nulabinc.backlog.r2b.interpreters.ConsoleDSL.ConsoleProgram
import com.nulabinc.backlog.r2b.interpreters.backlog.Backlog4jInterpreter
import monix.eval.Task

sealed trait AppADT[+A]
case class Pure[A](a: A)                      extends AppADT[A]
case class Backlog[A](prg: BacklogProgram[A]) extends AppADT[A]
case class Console[A](prg: ConsoleProgram[A]) extends AppADT[A]
case class Exit(exitCode: Int)                extends AppADT[Unit]

object AppDSL {

  type AppProgram[A] = Free[AppADT, A]

  def pure[A](a: A): AppProgram[A] =
    Free.liftF(Pure(a))

  def backlog[A](prg: BacklogProgram[A]): AppProgram[A] =
    Free.liftF[AppADT, A](Backlog(prg))

  def console[A](prg: ConsoleProgram[A]): AppProgram[A] =
    Free.liftF(Console(prg))

  def exit(reason: String, exitCode: Int): AppProgram[Unit] = {
    for {
      _ <- console(ConsoleDSL.print(reason))
      _ <- Free.liftF(Exit(exitCode))
    } yield ()
  }

}

case class AppInterpreter(backlogInterpreter: Backlog4jInterpreter, consoleInterpreter: ConsoleInterpreter) extends (AppADT ~> Task) {

  def run[A](prg: AppProgram[A]): Task[A] =
    prg.foldMap(this)

  override def apply[A](fa: AppADT[A]): Task[A] = fa match {
    case Pure(a)          => Task(a)
    case Backlog(prg)     => backlogInterpreter.run(prg)
    case Console(prg)     => prg.foldMap(consoleInterpreter)
    case Exit(statusCode) => sys.exit(statusCode)
  }
}
