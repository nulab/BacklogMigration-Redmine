package com.nulabinc.r2b.actor.utils

import akka.actor.{Props, ActorRef, Actor}

/**
 * @author uchida
 */
trait Subtasks {

  this: Actor =>

  var subtasks = Set.empty[ActorRef]

  /**
   * Create subtask (create, watch and add actor to subtask list).
   */
  def start(props: Props, name: String): ActorRef = {
    val subtask = context.watch(context.actorOf(props, name))
    subtasks += subtask
    subtask
  }

  /**
   * Complete subtask.
   */
  def complete(actor: ActorRef): Unit = subtasks -= context.unwatch(actor)

}
