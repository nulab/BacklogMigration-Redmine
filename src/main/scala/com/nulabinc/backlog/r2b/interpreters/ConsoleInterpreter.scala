package com.nulabinc.backlog.r2b.interpreters

import cats.free.Free
import cats.~>
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.osinka.i18n.Messages

import scala.concurrent.Future

sealed trait ConsoleADT[A]
case class Print(str: String) extends ConsoleADT[Unit]
case class Read() extends ConsoleADT[String]

object ConsoleDSL {

  type ConsoleProgram[A] = Free[ConsoleADT, A]

  def print(str: String): ConsoleProgram[Unit] =
    Free.liftF(Print(str))

  def read(): ConsoleProgram[String] =
    Free.liftF(Read())

}

class ConsoleInterpreter extends (ConsoleADT ~> Future) with Logging {

  def apply[A](fa: ConsoleADT[A]): Future[A] = fa match  {
    case Print(str) => Future.successful {
      ConsoleOut.println(str)
      ()
    }
    case Read() => Future.successful {
      scala.io.StdIn.readLine(Messages("cli.confirm"))
    }
  }
}
