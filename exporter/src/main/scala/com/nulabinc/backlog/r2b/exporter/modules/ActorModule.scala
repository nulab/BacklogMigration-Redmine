package com.nulabinc.backlog.r2b.exporter.modules

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.google.inject.name.{Named, Names}
import com.google.inject.{AbstractModule, Provides}
import com.nulabinc.backlog.migration.common.modules.akkaguice.GuiceAkkaActorRefProvider
import com.nulabinc.backlog.r2b.exporter.actor.{ContentActor, IssuesActor, WikisActor}
import net.codingwell.scalaguice.ScalaModule

/**
  * @author uchida
  */
private[exporter] class ActorModule extends AbstractModule with ScalaModule with GuiceAkkaActorRefProvider {

  override def configure() {
    bind[Actor].annotatedWith(Names.named(ContentActor.name)).to[ContentActor]
    bind[Actor].annotatedWith(Names.named(IssuesActor.name)).to[IssuesActor]
    bind[Actor].annotatedWith(Names.named(WikisActor.name)).to[WikisActor]
  }

  @Provides
  @Named(IssuesActor.name)
  def provideIssuesActorRef(@Inject() system: ActorSystem): ActorRef = provideActorRef(system, IssuesActor.name)

  @Provides
  @Named(WikisActor.name)
  def provideWikisActorRef(@Inject() system: ActorSystem): ActorRef = provideActorRef(system, WikisActor.name)

}
